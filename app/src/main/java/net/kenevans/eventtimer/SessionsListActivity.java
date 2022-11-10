package net.kenevans.eventtimer;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SessionsListActivity extends AppCompatActivity implements IConstants {
    //    private final List<LinkedListmSessionList = new ArrayList<>();
    ListView mListView;
    private EventTimerDbAdapter mDbAdapter;
    private SessionListAdapter mSessionListAdapter;
//    private RestoreTask mRestoreTask;

    // Launcher for PREF_TREE_URI
    private final ActivityResultLauncher<Intent> openDocumentTreeLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        Log.d(TAG, "openDocumentTreeLauncher: result" +
                                ".getResultCode()=" + result.getResultCode());
                        // Find the UID for this application
                        Log.d(TAG, "URI=" + UriUtils.getApplicationUid(this));
                        Log.d(TAG,
                                "Current permissions (initial): "
                                        + UriUtils.getNPersistedPermissions(this));
                        try {
                            if (result.getResultCode() == RESULT_OK) {
                                // Get Uri from Storage Access Framework.
                                Uri treeUri = result.getData().getData();
                                SharedPreferences.Editor editor =
                                        getPreferences(MODE_PRIVATE)
                                                .edit();
                                if (treeUri == null) {
                                    editor.putString(PREF_TREE_URI, null);
                                    editor.apply();
                                    Utils.errMsg(this, "Failed to get " +
                                            "persistent " +
                                            "access permissions");
                                    return;
                                }
                                // Persist access permissions.
                                try {
                                    this.getContentResolver().takePersistableUriPermission(treeUri,
                                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                                    // Save the current treeUri as PREF_TREE_URI
                                    editor.putString(PREF_TREE_URI,
                                            treeUri.toString());
                                    editor.apply();
                                    // Trim the persisted permissions
                                    UriUtils.trimPermissions(this, 1);
                                } catch (Exception ex) {
                                    String msg = "Failed to " +
                                            "takePersistableUriPermission for "
                                            + treeUri.getPath();
                                    Utils.excMsg(this, msg, ex);
                                }
                                Log.d(TAG,
                                        "Current permissions (final): "
                                                + UriUtils.getNPersistedPermissions(this));
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, "Error in openDocumentTreeLauncher: " +
                                    "startActivityForResult", ex);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sessions_list);
        mListView = findViewById(R.id.sessionsListView);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mSessionListAdapter = new SessionListAdapter(this, mDbAdapter);
        FloatingActionButton fab =
                findViewById(R.id.fab);
        fab.setOnClickListener(view -> addNewSession());

        mDbAdapter = new EventTimerDbAdapter(this);
        mDbAdapter.open();
    }

    @Override
    protected void onPause() {
        Log.v(TAG, this.getClass().getSimpleName() + " onPause");
        super.onPause();
        if (mSessionListAdapter != null) {
            mSessionListAdapter.clear();
        }
    }

    @Override
    public void onResume() {
        Log.d(TAG, this.getClass().getSimpleName() + " onResume:");
        super.onResume();
        refresh();

//        // Check if PREF_TREE_URI is valid and remove it if not
//        if (UriUtils.getNPersistedPermissions(this) <= 0) {
//            SharedPreferences.Editor editor =
//                    getPreferences(MODE_PRIVATE)
//                            .edit();
//            editor.putString(PREF_TREE_URI, null);
//            editor.apply();
//        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_session_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item.getItemId() == R.id.menu_discard) {
            promptToDiscardSession();
            return true;
        } else if (item.getItemId() == R.id.menu_save) {
            saveSessions();
            return true;
        } else if (item.getItemId() == R.id.menu_check_all) {
            setAllSessionsChecked(true);
            return true;
        } else if (item.getItemId() == R.id.menu_check_none) {
            setAllSessionsChecked(false);
            return true;
        } else if (item.getItemId() == R.id.info) {
            info();
            return true;
        } else if (item.getItemId() == R.id.menu_save_database) {
            saveDatabase();
            return true;
        } else if (item.getItemId() == R.id.menu_replace_database) {
            checkReplaceDatabase();
            return true;
        } else if (item.getItemId() == R.id.choose_data_directory) {
            chooseDataDirectory();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // This seems to be necessary with Android 12
        // Otherwise onDestroy is not called
        Log.d(TAG, this.getClass().getSimpleName() + ": onBackPressed");
        finish();
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, this.getClass().getSimpleName() + ": onDestroy");
        super.onDestroy();
        if (mDbAdapter != null) {
            mDbAdapter.close();
            mDbAdapter = null;
        }
    }

    private void addNewSession() {
        Log.d(TAG, "FloatingActionButton onClick nSessions="
                + mSessionListAdapter.mSessions.size()
                + " thread=" + Thread.currentThread());
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        input.setText(dateFormat.format(new Date()));
        builder.setView(input);

        builder.setTitle("Enter the new session name");
        // Set up the buttons
        builder.setPositiveButton(android.R.string.ok,
                (dialog, which) -> {
                    dialog.dismiss();
                    addSession(input.getText().toString());
                });
        builder.setNegativeButton(android.R.string.cancel,
                (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void addSession(String name) {
        long now = new Date().getTime();
        // Add at beginning
        Session session = new Session(mDbAdapter, now);
        if (name != null) {
            session.setName(mDbAdapter, name);
        }
        mSessionListAdapter.addSession(new SessionDisplay(session.getId(),
                session.getName(), session.getStartTime(),
                session.getEndTime(), session.getEventList().size()));

        refresh();
    }

    /**
     * Splits the selected sessions.
     */
    public void splitSessions() {
        Utils.infoMsg(this, "Not implemented yet");
    }

    /**
     * Sets all the sessions to checked or not.
     *
     * @param checked Whether checked or not.
     */
    public void setAllSessionsChecked(Boolean checked) {
        ArrayList<SessionDisplay> sessions = mSessionListAdapter.getSessions();
        CheckBox cb;
        for (SessionDisplay session : sessions) {
            session.setChecked(checked);
            cb = session.getCheckBox();
            if (cb != null) {
                cb.setChecked(checked);
            }
        }
    }

    /**
     * Saves the selected sessions.
     */
    public void saveSessions() {
        ArrayList<SessionDisplay> checkedSessions = mSessionListAdapter
                .getCheckedSessions();
        if (checkedSessions.size() == 0) {
            Utils.errMsg(this, "There are no sessions to save");
            return;
        }
        // Get the saved tree Uri
        SharedPreferences prefs = getSharedPreferences(MAIN_ACTIVITY,
                MODE_PRIVATE);
        String treeUriStr = prefs.getString(PREF_TREE_URI, null);
        if (treeUriStr == null) {
            Utils.errMsg(this, "There is no data directory set");
            return;
        }
        // Get a docTree Uri
        Uri treeUri = Uri.parse(treeUriStr);
        String treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri);
        int nErrors = 0;
        int nWriteErrors;
        String errMsg = "Error saving sessions:\n";
        String fileNames = "Saved to:\n";
        String fileName;
        String name;
        long sessionId;
        for (SessionDisplay session : checkedSessions) {
            try {
                sessionId = session.getId();
                name = session.getSafeName();
                fileName = SESSION_CSV_NAME_PREFIX + name + ".csv";
                Uri docTreeUri =
                        DocumentsContract.buildDocumentUriUsingTree(treeUri,
                                treeDocumentId);
                // Get a docUri and ParcelFileDescriptor
                ContentResolver resolver = this.getContentResolver();
                ParcelFileDescriptor pfd;
                // Create the document
                Uri docUri = DocumentsContract.createDocument(resolver,
                        docTreeUri,
                        "test/csv", fileName);
                pfd = getContentResolver().
                        openFileDescriptor(docUri, "w");
                try (FileWriter writer =
                             new FileWriter(pfd.getFileDescriptor());
                     BufferedWriter out = new BufferedWriter(writer)) {
                    // Write the session data
                    nWriteErrors = writeSessionDataToCvsFile(sessionId, out);
                    if (nWriteErrors > 0) {
                        nErrors += nWriteErrors;
                        errMsg += "  " + session.getName();
                    }
                    fileNames += "  " + docUri.getLastPathSegment() + "\n";
                }
            } catch (Exception ex) {
                nErrors++;
                errMsg += "  " + session.getName();
            }
        }
        String msg = "";
        if (nErrors > 0) {
            msg += errMsg;
        }
        msg += fileNames;
        if (nErrors > 0) {
            Utils.errMsg(this, msg);
        } else {
            Utils.infoMsg(this, msg);
        }
    }

    /**
     * Writes the session data for the given startTime to the given
     * BufferedWriter.
     *
     * @param sessionId The session id.
     * @param out       The BufferedWriter.
     * @return The number of errors.
     */
    private int writeSessionDataToCvsFile(long sessionId, BufferedWriter out) {
        int nErrors = 0;
        if (mDbAdapter == null) {
            Log.d(TAG, "writeSessionDataToCvsFile: database adapter is null");
            nErrors++;
        }
        if (out == null) {
            Log.d(TAG, "writeSessionDataToCvsFile: BufferedWriter is null");
            nErrors++;
        }
        if (nErrors > 0) {
            return nErrors;
        }
        Session session = Session.getSessionFromDb(mDbAdapter, sessionId);
        try {
            String sessionName = session.getName();
            Date startTime = new Date(session.getStartTime());
            String startStr = dateFormat.format(startTime);
            String endStr;
            if (session.getEndTime() == INVALID_TIME) {
                endStr = "NA";
            } else {
                Date endTime = new Date(session.getEndTime());
                endStr = csvDateFormat.format(endTime);
            }
            String nameStr = sessionName;
            if (sessionName == null || sessionName.length() == 0) {
                nameStr = "Not Named";
            }
            int nEvents = session.getEventList().size();
            String nEventsStr;
            nEventsStr = String.format(Locale.US, "%d", nEvents);
            String durationStr = "NA";
            if (startTime.getTime() != INVALID_TIME) {
                if (session.getEndTime() != INVALID_TIME) {
                    durationStr =
                            Utils.getDurationString(session.getStartTime(),
                                    session.getEndTime());
                }
            }
            out.write("Start Time" + CSV_DELIM + startStr + "\n");
            out.write("End Time" + CSV_DELIM + endStr + "\n");
            out.write("Events" + CSV_DELIM + nEventsStr + "\n");
            out.write("Duration" + CSV_DELIM + durationStr + "\n");
            out.write("Name" + CSV_DELIM + nameStr + "\n");

            // Loop over events
            String line, note;
            long time;
            out.write(COL_TIME + CSV_DELIM + COL_NOTE + CSV_DELIM
                    + "java-" + COL_TIME + "\n");
            for (Event event : session.getEventList()) {
                time = event.getTime();
                line = dateFormat.format(time) + CSV_DELIM;
                note = event.getNote();
                line += note + CSV_DELIM;
                line += time + "\n";
                out.write(line);
            }
        } catch (Exception ex) {
            nErrors++;
        }
        return nErrors;
    }

    /**
     * Prompts to discards the selected sessions. The method doDiscard will do
     * the actual discarding, if the user confirms.
     *
     * @see #doDiscardSession()
     */
    public void promptToDiscardSession() {
        ArrayList<SessionDisplay> checkedSessions = mSessionListAdapter
                .getCheckedSessions();
        if (checkedSessions.size() == 0) {
            Utils.errMsg(this, "There are no sessions to discard");
            return;
        }
        String msg = SessionsListActivity.this.getString(
                R.string.session_delete_prompt, checkedSessions.size());
        new AlertDialog.Builder(SessionsListActivity.this)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.confirm)
                .setMessage(msg)
                .setPositiveButton(R.string.ok,
                        (dialog, which) -> {
                            dialog.dismiss();
                            doDiscardSession();
                        }).setNegativeButton(R.string.cancel, null).show();
    }

    /**
     * Does the actual work of discarding the selected sessions.
     */
    public void doDiscardSession() {
        ArrayList<SessionDisplay> checkedSessions = mSessionListAdapter
                .getCheckedSessions();
        if (checkedSessions.size() == 0) {
            Utils.errMsg(this, "There are no sessions to discard");
            return;
        }
        long sessionId;
        for (SessionDisplay session : checkedSessions) {
            sessionId = session.getId();
            boolean success = mDbAdapter.deleteDataForSession(sessionId);
            if (!success) {
                String msg = "Failed to delete all or part of Session "
                        + session.getName();
                Log.d(TAG, msg);
                Utils.errMsg(this, msg);
            }
        }
        refresh();
    }

    /**
     * Displays info about the current configuration
     */
    private void info() {
        try {
            StringBuilder info = new StringBuilder();
            if (mSessionListAdapter != null) {
                info.append("Number of sessions: ")
                        .append(mSessionListAdapter.getCount()).append("\n");
            } else {
                info.append("Number of sessions: NA").append("\n");
            }
            SharedPreferences prefs = getPreferences(MODE_PRIVATE);
            info.append(UriUtils.getRequestedPermissionsInfo(this));
            String treeUriStr = prefs.getString(PREF_TREE_URI, null);
            if (treeUriStr == null) {
                info.append("Data Directory: Not set");
            } else {
                Uri treeUri = Uri.parse(treeUriStr);
                if (treeUri == null) {
                    info.append("Data Directory: Not set");
                } else {
                    info.append("Data Directory: ").append(treeUri.getPath());
                }
            }
            Utils.infoMsg(this, info.toString());
        } catch (Throwable t) {
            Utils.excMsg(this, "Error showing info", t);
            Log.e(TAG, "Error showing info", t);
        }
    }

    /**
     * Sets the current data directory
     */
    private void chooseDataDirectory() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION &
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        openDocumentTreeLauncher.launch(intent);
    }

    private void saveDatabase() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        String treeUriStr = prefs.getString(PREF_TREE_URI, null);
        if (treeUriStr == null) {
            Utils.errMsg(this, "There is no data directory set");
            return;
        }
        try {
            String format = "yyyy-MM-dd-HHmmss";
            SimpleDateFormat df = new SimpleDateFormat(format, Locale.US);
            Date now = new Date();
            String fileName = String.format(saveDatabaseTemplate,
                    df.format(now));
            Uri treeUri = Uri.parse(treeUriStr);
            String treeDocumentId =
                    DocumentsContract.getTreeDocumentId(treeUri);
            Uri docTreeUri =
                    DocumentsContract.buildDocumentUriUsingTree(treeUri,
                            treeDocumentId);
            ContentResolver resolver = this.getContentResolver();
            Uri docUri = DocumentsContract.createDocument(resolver, docTreeUri,
                    "application/vnd.sqlite3", fileName);
            if (docUri == null) {
                Utils.errMsg(this, "Could not create document Uri");
                return;
            }
            ParcelFileDescriptor pfd = getContentResolver().
                    openFileDescriptor(docUri, "rw");
            File src = new File(getExternalFilesDir(null), DB_NAME);
            Log.d(TAG, "saveDatabase: docUri=" + docUri);
            try (FileChannel in =
                         new FileInputStream(src).getChannel();
                 FileChannel out =
                         new FileOutputStream(pfd.getFileDescriptor()).getChannel()) {
                out.transferFrom(in, 0, in.size());
            } catch (Exception ex) {
                String msg = "Error copying source database from "
                        + docUri.getLastPathSegment() + " to "
                        + src.getPath();
                Log.e(TAG, msg, ex);
                Utils.excMsg(this, msg, ex);
            }
            Utils.infoMsg(this, "Wrote " + docUri.getLastPathSegment());
        } catch (Exception ex) {
            String msg = "Error saving to SD card";
            Utils.excMsg(this, msg, ex);
            Log.e(TAG, msg, ex);
        }
    }

    /**
     * Does the preliminary checking for restoring the database, prompts if
     * it is OK to delete the current one, and call restoreDatabase to
     * actually
     * do the replace.
     */
    private void checkReplaceDatabase() {
        Log.d(TAG, "checkReplaceDatabase");
        // Find the .db files in the data directory
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        String treeUriStr = prefs.getString(PREF_TREE_URI, null);
        if (treeUriStr == null) {
            Utils.errMsg(this, "There is no tree Uri set");
            return;
        }
        Uri treeUri = Uri.parse(treeUriStr);
        final List<UriUtils.UriData> children =
                UriUtils.getChildren(this, treeUri, ".db");
        final int len = children.size();
        if (len == 0) {
            Utils.errMsg(this, "There are no .db files in the data directory");
            return;
        }
        // Sort them by date with newest first
        Collections.sort(children,
                (data1, data2) -> Long.compare(data2.modifiedTime,
                        data1.modifiedTime));

        // Prompt for the file to use
        final CharSequence[] items = new CharSequence[children.size()];
        String displayName;
        UriUtils.UriData uriData;
        for (int i = 0; i < len; i++) {
            uriData = children.get(i);
            displayName = uriData.displayName;
            if (displayName == null) {
                displayName = uriData.uri.getLastPathSegment();
            }
            items[i] = displayName;
        }
        androidx.appcompat.app.AlertDialog.Builder builder =
                new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle(getText(R.string.select_replace_database));
        builder.setSingleChoiceItems(items, 0,
                (dialog, item) -> {
                    dialog.dismiss();
                    if (item < 0 || item >= len) {
                        Utils.errMsg(SessionsListActivity.this,
                                "Invalid item");
                        return;
                    }
                    // Confirm the user wants to delete all the current data
                    new androidx.appcompat.app.AlertDialog.Builder(SessionsListActivity.this)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle(R.string.confirm)
                            .setMessage(R.string.delete_prompt)
                            .setPositiveButton(R.string.ok,
                                    (dialog1, which) -> {
                                        dialog1.dismiss();
                                        Log.d(TAG, "Calling replaceDatabase: " +
                                                "uri="
                                                + children.get(item).uri);
                                        replaceDatabase(children.get(item).uri);
                                    })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                });
        androidx.appcompat.app.AlertDialog alert = builder.create();
        alert.show();
    }

    /**
     * Replaces the database without prompting.
     *
     * @param uri The Uri.
     */
    private void replaceDatabase(Uri uri) {
        Log.d(TAG, this.getClass().getSimpleName() + ": replaceDatabase: uri="
                + uri.getLastPathSegment());
        String lastSeg = uri.getLastPathSegment();
        if (!UriUtils.exists(this, uri)) {
            String msg = "Source database does not exist " + lastSeg;
            Log.d(TAG, "replaceDatabase: " + msg);
            Utils.errMsg(this, msg);
            return;
        }
        // Copy the data base to app storage
        File dest = null;
        try {
            String destFileName = UriUtils.getFileNameFromUri(uri);
            dest = new File(getExternalFilesDir(null), destFileName);
            dest.createNewFile();
            ParcelFileDescriptor pfd = getContentResolver().
                    openFileDescriptor(uri, "rw");
            try (FileChannel in =
                         new FileInputStream(pfd.getFileDescriptor()).getChannel();
                 FileChannel out =
                         new FileOutputStream(dest).getChannel()) {
                out.transferFrom(in, 0, in.size());
            } catch (Exception ex) {
                String msg = "Error copying source database from "
                        + uri.getLastPathSegment() + " to "
                        + dest.getPath();
                Log.e(TAG, msg, ex);
                Utils.excMsg(this, msg, ex);
            }
        } catch (Exception ex) {
            String msg = "Error getting source database" + uri;
            Log.e(TAG, msg, ex);
            Utils.excMsg(this, msg, ex);
        }
        try {
            // Replace (Use null for default alias)
            mDbAdapter.replaceDatabase(dest.getPath(), null);
            Utils.infoMsg(this,
                    "Restored database from " + uri.getLastPathSegment());
        } catch (Exception ex) {
            String msg = "Error replacing data from " + dest.getPath();
            Log.e(TAG, msg, ex);
            Utils.excMsg(this, msg, ex);
        }
    }

    /**
     * Get the list of available restore files.
     *
     * @param context The context.
     * @return The list.
     */
    public static List<UriUtils.UriData> getUriList(Context context) {
        ContentResolver contentResolver = context.getContentResolver();
        SharedPreferences prefs = context.getSharedPreferences(
                "SessionsListActivity", Context.MODE_PRIVATE);
        String treeUriStr = prefs.getString(PREF_TREE_URI, null);
        if (treeUriStr == null) {
            Utils.errMsg(context, "There is no tree Uri set");
            return null;
        }
        Uri treeUri = Uri.parse(treeUriStr);
        Uri childrenUri =
                DocumentsContract.buildChildDocumentsUriUsingTree(treeUri,
                        DocumentsContract.getTreeDocumentId(treeUri));
        List<UriUtils.UriData> uriList = new ArrayList<>();
        String[] projection = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
        };

        try (Cursor cursor = contentResolver.query(childrenUri, projection,
                null, null, null)) {
            if (cursor == null) return null;
            String documentId;
            Uri documentUri;
            String displayName;
            long lastModified;
            while (cursor.moveToNext()) {
                documentUri = null;
                if (cursor.getColumnIndex(projection[0]) != -1) {
                    documentId = cursor.getString(0);
                    documentUri =
                            DocumentsContract.buildDocumentUriUsingTree(treeUri,
                                    documentId);
                }
                if (documentUri == null) continue;

                displayName = "<NA>";
                if (cursor.getColumnIndex(projection[1]) != -1) {
                    displayName = cursor.getString(1);
                }
                lastModified = -1;
                if (cursor.getColumnIndex(projection[2]) != -1) {
                    lastModified = cursor.getLong(2);
                }
                if (displayName.startsWith(SAVE_DATABASE_FILENAME_PREFIX)
                        && displayName.endsWith(SAVE_DATABASE_FILENAME_SUFFIX)) {
                    uriList.add(new UriUtils.UriData(documentUri, lastModified,
                            displayName));
                }
            }
        }
        return uriList;
    }

    /**
     * Refreshes the sessions by recreating the list adapter.
     */
    public void refresh() {
        // Initialize the list view adapter
        mSessionListAdapter = new SessionListAdapter(this, mDbAdapter);
        mListView.setAdapter(mSessionListAdapter);
    }

//    /**
//     * Class to handle restore using a progress bar that can be cancelled.<br>
//     * <br>
//     * Call with <b>Bitmap bitmap = new MyUpdateTask().execute(String)<b>
//     */
//    private class RestoreTask extends AsyncTask<Void, Void, Boolean> {
//        private ProgressDialog dialog;
//        private Context mCtx;
//        private Uri mUri;
//        private int mErrors;
//        private int mLineNumber;
//        private String mExceptionMsg;
//
//        private RestoreTask(Context context, Uri uri) {
//            super();
//            this.mCtx = context;
//            this.mUri = uri;
//        }
//
//        @Override
//        protected void onPreExecute() {
//            dialog = new ProgressDialog(SessionsListActivity.this);
//            dialog.setMessage(getString(R.string
//                    .restoring_database_progress_text));
//            dialog.setCancelable(false);
//            dialog.setIndeterminate(true);
//            dialog.show();
//        }
//
//        @Override
//        protected Boolean doInBackground(Void... dummy) {
//            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
//            try {
//                // Delete all the data and recreate the table
//                mDbAdapter.recreateDataTable();
//                try (InputStreamReader inputStreamReader =
//                             new InputStreamReader(
//                                     mCtx.getContentResolver()
//                                     .openInputStream(mUri));
//                     BufferedReader in =
//                             new BufferedReader(inputStreamReader)) {
//                    // Read the file and get the data to restore
//                    String rr;
//                    long dateNum, startDateNum;
//                    int hr;
//                    String[] tokens;
//                    String line;
//                    while ((line = in.readLine()) != null) {
//                        dateNum = startDateNum = INVALID_TIME;
//                        mLineNumber++;
//                        tokens = line.trim().split(SAVE_DATABASE_DELIM);
//                        // Skip blank lines
//                        if (line.trim().length() == 0) {
//                            continue;
//                        }
//                        // Skip lines starting with #
//                        if (tokens[0].trim().startsWith("#")) {
//                            continue;
//                        }
//                        hr = 0;
//                        if (tokens.length < 4) {
//                            // Utils.errMsg(this, "Found " + tokens.length
//                            // + " tokens for line " + lineNum
//                            // + "\nShould be 5 or more tokens");
//                            mErrors++;
//                            Log.d(TAG, "tokens.length=" + tokens.length
//                                    + " @ line " + mLineNumber);
//                            Log.d(TAG, line);
//                            continue;
//                        }
//                        try {
//                            dateNum = Long.parseLong(tokens[0]);
//                        } catch (Exception ex) {
//                            Log.d(TAG, "Long.parseLong failed for dateNum @
//                            " +
//                                    "line "
//                                    + mLineNumber);
//                        }
//                        try {
//                            startDateNum = Long.parseLong(tokens[1]);
//                        } catch (Exception ex) {
//                            Log.d(TAG,
//                                    "Long.parseLong failed for startDateNum
//                                    @" +
//                                            " line "
//                                            + mLineNumber);
//                        }
//                        try {
//                            hr = Integer.parseInt(tokens[2]);
//                        } catch (Exception ex) {
//                            Log.d(TAG, "Integer.parseInt failed for hr @
//                            line "
//                                    + mLineNumber);
//                        }
//                        rr = tokens[3].trim();
//                        // Write the row
//                        long id = mDbAdapter.createData(dateNum, startDateNum
//                                , hr,
//                                rr);
//                        if (id < 0) {
//                            mErrors++;
//                        }
//                    }
//                }
//            } catch (Exception ex) {
//                mExceptionMsg = "Got Exception restoring at line "
//                        + mLineNumber + "\n" + ex.getMessage();
//            }
//            return true;
//        }
//    }
}