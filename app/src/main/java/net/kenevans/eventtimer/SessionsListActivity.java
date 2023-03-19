package net.kenevans.eventtimer;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
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
    private Uri mUriToAdd;
//    private RestoreTask mRestoreTask;

    // Launcher for adding session from CSV
    private final ActivityResultLauncher<Intent> openCsvLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        Log.d(TAG, "openCsvLauncher: result" +
                                ".getResultCode()=" + result.getResultCode());
                        if (result.getResultCode() != RESULT_OK ||
                                result.getData() == null) {
                            Utils.warnMsg(this, "Failed to get CSV file");
                        } else {
                            // Set flag to add it in onResume
                            mUriToAdd = result.getData().getData();
                        }
                    });


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
                            if (result.getResultCode() == RESULT_OK &&
                                    result.getData() != null) {
                                // Get Uri from Storage Access Framework.
                                Uri treeUri = result.getData().getData();
                                SharedPreferences.Editor editor =
                                        getPreferences(MODE_PRIVATE)
                                                .edit();
                                if (treeUri == null) {
                                    editor.putString(PREF_TREE_URI, null);
                                    editor.apply();
                                    Utils.errMsg(this, "Failed to get " +
                                            "persistent access permissions");
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
        if (mUriToAdd != null) {
            doAddSessionFromCsvFile(mUriToAdd);
            refresh();
            mUriToAdd = null;
        }

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
        int id = item.getItemId();
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (id == R.id.menu_discard) {
            promptDiscardSession();
            return true;
        } else if (id == R.id.menu_copy_summaries) {
            copySummaries();
            return true;
        } else if (id == R.id.menu_save_summaries) {
            saveSummaries();
            return true;
        } else if (id == R.id.menu_save) {
            saveSessionsToCsv();
            return true;
        } else if (id == R.id.menu_check_all) {
            setAllSessionsChecked(true);
            return true;
        } else if (id == R.id.menu_check_none) {
            setAllSessionsChecked(false);
            return true;
        } else if (id == R.id.menu_session_from_cvs) {
            promptAddSessionFromCsvFile();
            return true;
        } else if (id == R.id.info) {
            info();
            return true;
        } else if (id == R.id.help) {
            showHelp();
            return true;
        } else if (id == R.id.menu_save_database) {
            saveDatabase();
            return true;
        } else if (id == R.id.menu_replace_database) {
            promptReplaceDatabase();
            return true;
        } else if (id == R.id.choose_data_directory) {
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
        LinearLayout ll = new LinearLayout(this);
        int padding = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                10,
                getResources().getDisplayMetrics()
        );
        ll.setPadding(padding, padding, padding, padding);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams llParam = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        llParam.gravity = Gravity.CENTER;
        ll.setLayoutParams(llParam);

        llParam =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
        llParam.gravity = Gravity.CENTER;
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setText(dateOnlyFormat.format(new Date()));
        ll.addView(input);

        CheckBox cb = new CheckBox(this);
        cb.setChecked(false);
        cb.setText(R.string.next_day);
        cb.setOnCheckedChangeListener((button, isChecked) -> {
            Log.d(TAG, "onCheckedChanged: isChecked=" + isChecked);
            if(isChecked) {
                Date date = new Date();
                long ms = date.getTime() + 86400000;
                date.setTime(ms);
                input.setText(dateOnlyFormat.format(date));
            } else {
                input.setText(dateOnlyFormat.format(new Date()));
            }
        });
        ll.addView(cb);

        builder.setView(ll);
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
                session.getName(), session.getFirstEventTime(),
                session.getEndTime(), session.getEventList().size(),
                session.getDuration()));

        refresh();
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
     * Saves the selected sessions summaries as text.
     */
    public void saveSummaries() {
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
        StringBuilder errMsg = new StringBuilder("Error saving summaries:\n");
        StringBuilder fileNames = new StringBuilder("Saved to:\n");
        String fileName;
        String name;
        long sessionId;
        for (SessionDisplay session : checkedSessions) {
            try {
                sessionId = session.getId();
                name = session.getSafeName();
                fileName = SESSION_CSV_NAME_PREFIX + name + ".txt";
                Uri docTreeUri =
                        DocumentsContract.buildDocumentUriUsingTree(treeUri,
                                treeDocumentId);
                // Get a docUri and ParcelFileDescriptor
                ContentResolver resolver = this.getContentResolver();
                ParcelFileDescriptor pfd;
                Uri docUri = DocumentsContract.createDocument(resolver,
                        docTreeUri,
                        "text/plain", fileName);
                pfd = getContentResolver().
                        openFileDescriptor(docUri, "w");
                try (FileWriter writer =
                             new FileWriter(pfd.getFileDescriptor());
                     BufferedWriter out = new BufferedWriter(writer)) {
                    // Write the session data
                    nWriteErrors = doSaveSingleSummary(sessionId, out);
                    if (nWriteErrors > 0) {
                        nErrors += nWriteErrors;
                        errMsg.append("  ").append(session.getName());
                    }
                    fileNames.append("  ").append(docUri.getLastPathSegment()).append("\n");
                }
            } catch (Exception ex) {
                nErrors++;
                errMsg.append("  ").append(session.getName());
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
     * Copies the selected sessions summaries to the clipboard.
     */
    public void copySummaries() {
        if (mDbAdapter == null) {
            Log.d(TAG, "copySummaries: database adapter is null");
        }
        ArrayList<SessionDisplay> checkedSessions = mSessionListAdapter
                .getCheckedSessions();
        if (checkedSessions.size() == 0) {
            Utils.errMsg(this, "There are no sessions to copy");
            return;
        }
        int nErrors = 0;
        long sessionId;
        Session session = null;
        StringBuilder errMsg = new StringBuilder("Error copying summaries:\n");
        StringBuilder sb = new StringBuilder();
        for (SessionDisplay sessionDisplay : checkedSessions) {
            try {
                sessionId = sessionDisplay.getId();
                session = Session.getSessionFromDb(mDbAdapter, sessionId);
                long startTime = session.getFirstEventTime();
                String startTimeStr = dateFormat.format(startTime);
//            String endTimeStr = dateFormat.format(endTime);
                String sessionName = sessionDisplay.getName();
                String nameStr = sessionName;
                if (sessionName == null || sessionName.length() == 0) {
                    nameStr = "Not Named";
                }
                int nEvents = session.getEventList().size();
                String nEventsStr;
                nEventsStr = String.format(Locale.US, "%d", nEvents);
                String durationStr = sessionDisplay.getDuration();
                sb.append("Name: ").append(nameStr).append("\n");
                sb.append("Start Time: ").append(startTimeStr).append("\n");
                sb.append("Events: ").append(nEventsStr).append("\n");
                sb.append("Duration: ").append(durationStr).append("\n");

                // Loop over events
                String line, note;
                long time;
                EventEx eventEx;
                for (Event event : session.getEventList()) {
                    time = event.getTime();
                    eventEx = new EventEx(mDbAdapter, event);
                    durationStr = "[" + eventEx.getDuration() + "]";
                    line = summaryDateFormat.format(time) + " ";
                    note = event.getNote();
                    line += String.format(Locale.US, "%8s ", durationStr);
                    line += note + "\n";
                    sb.append(line);
                }
                if(checkedSessions.size() > 1) {
                    sb.append("\n");
                }
            } catch (Exception ex) {
                nErrors++;
                if (session != null) {
                    errMsg.append("  ").append(session.getName());
                    errMsg.append("\n");
                }
            }
        }
        if (nErrors > 0) {
            Utils.errMsg(this, errMsg.toString());
        }
        ClipboardManager cbm =
                (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData data = ClipData.newPlainText("Session Summaries",
                sb.toString());
        cbm.setPrimaryClip(data);
    }

    /**
     * Saves the selected sessions as tab-delimited CSV.
     */
    public void saveSessionsToCsv() {
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
        StringBuilder errMsg = new StringBuilder("Error saving sessions:\n");
        StringBuilder fileNames = new StringBuilder("Saved to:\n");
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
                Uri docUri = DocumentsContract.createDocument(resolver,
                        docTreeUri,
                        "text/csv", fileName);
                pfd = getContentResolver().
                        openFileDescriptor(docUri, "w");
                try (FileWriter writer =
                             new FileWriter(pfd.getFileDescriptor());
                     BufferedWriter out = new BufferedWriter(writer)) {
                    // Write the session data
                    nWriteErrors = doSaveSingleSessionToCsv(sessionId, out);
                    if (nWriteErrors > 0) {
                        nErrors += nWriteErrors;
                        errMsg.append("  ").append(session.getName());
                    }
                    fileNames.append("  ").append(docUri.getLastPathSegment()).append("\n");
                }
            } catch (Exception ex) {
                nErrors++;
                errMsg.append("  ").append(session.getName());
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
    private int doSaveSingleSummary(long sessionId, BufferedWriter out) {
        int nErrors = 0;
        if (mDbAdapter == null) {
            Log.d(TAG, "doSaveSessionToCsv: database adapter is null");
            nErrors++;
        }
        if (out == null) {
            Log.d(TAG, "doSaveSessionToCsv: BufferedWriter is null");
            nErrors++;
        }
        if (nErrors > 0) {
            return nErrors;
        }
        Session session;
        try {
            session = Session.getSessionFromDb(mDbAdapter, sessionId);
            long startTime = session.getFirstEventTime();
            String startTimeStr = dateFormat.format(startTime);
//            String endTimeStr = dateFormat.format(endTime);
            String sessionName = session.getName();
            String nameStr = sessionName;
            if (sessionName == null || sessionName.length() == 0) {
                nameStr = "Not Named";
            }
            int nEvents = session.getEventList().size();
            String nEventsStr;
            nEventsStr = String.format(Locale.US, "%d", nEvents);
            String durationStr = session.getDuration();
            out.write("Name: " + nameStr + "\n");
            out.write("Start Time: " + startTimeStr + "\n");
//            out.write("End Time: " + endTimeStr + "\n");
            out.write("Events: " + nEventsStr + "\n");
            out.write("Duration: " + durationStr + "\n");

            // Loop over events
            String line, note;
            long time;
            EventEx eventEx;
            for (Event event : session.getEventList()) {
                time = event.getTime();
                eventEx = new EventEx(mDbAdapter, event);
                durationStr = "[" + eventEx.getDuration() + "]";
                line = summaryDateFormat.format(time) + " ";
                note = event.getNote();
                line += String.format(Locale.US, "%8s ", durationStr);
                line += note + "\n";
                out.write(line);
            }
        } catch (Exception ex) {
            nErrors++;
        }
        return nErrors;
    }

    /**
     * Writes the session data for the given startTime to the given
     * BufferedWriter.
     *
     * @param sessionId The session id.
     * @param out       The BufferedWriter.
     * @return The number of errors.
     */
    private int doSaveSingleSessionToCsv(long sessionId, BufferedWriter out) {
        int nErrors = 0;
        if (mDbAdapter == null) {
            Log.d(TAG, "doSaveSessionToCsv: database adapter is null");
            nErrors++;
        }
        if (out == null) {
            Log.d(TAG, "doSaveSessionToCsv: BufferedWriter is null");
            nErrors++;
        }
        if (nErrors > 0) {
            return nErrors;
        }
        Session session;
        try {
            session = Session.getSessionFromDb(mDbAdapter, sessionId);
            long createTime = session.getCreateTime();
            long startTime = session.getFirstEventTime();
            long endTime = session.getEndTime();
            String createTimeStr = dateFormat.format(createTime);
            String startTimeStr = dateFormat.format(startTime);
            String endTimeStr = dateFormat.format(endTime);
            String sessionName = session.getName();
            String nameStr = sessionName;
            if (sessionName == null || sessionName.length() == 0) {
                nameStr = "Not Named";
            }
            int nEvents = session.getEventList().size();
            String nEventsStr;
            nEventsStr = String.format(Locale.US, "%d", nEvents);
            String durationStr = session.getDuration();
            out.write("Create Time" + CSV_DELIM + createTimeStr + CSV_DELIM + createTime + "\n");
            out.write("Start Time" + CSV_DELIM + startTimeStr + CSV_DELIM + startTime + "\n");
            out.write("End Time" + CSV_DELIM + endTimeStr + CSV_DELIM + endTime + "\n");
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
    public void promptDiscardSession() {
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
            boolean success = mDbAdapter.deleteSessionAndData(sessionId);
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
     * Opens a CSV file picked by the system file chooser.
     */
    void promptAddSessionFromCsvFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
//        intent.setType("*/*");
        intent.setType("text/comma-separated-values");
        openCsvLauncher.launch(intent);
    }

    private Session doAddSessionFromCsvFile(Uri uri) {
        Session session = null;
        List<Event> eventList;
        int lineNum = 0;
        try {
            try (InputStreamReader inputStreamReader =
                         new InputStreamReader(
                                 this.getContentResolver().openInputStream(uri));
                 BufferedReader in =
                         new BufferedReader(inputStreamReader)) {
                // Read the file and get the data to restore
                long createTime = INVALID_TIME;
                long time;
                long sessionId = -1;
                String name = "", note;
                boolean createTimeDefined = false, nameDefined = false;
                String[] tokens;
                String line;
                boolean doHeader = true;
                while ((line = in.readLine()) != null) {
                    lineNum++;
                    tokens = line.trim().split(CSV_DELIM);
                    if (tokens.length == 0) {
                        // Empty line
                        continue;
                    }
                    if (tokens[0].trim().startsWith("#")) {
                        // Comment
                        continue;
                    }
                    if (doHeader) {
                        if (tokens[0].trim().startsWith("Create Time")) {
                            createTime = Long.parseLong(tokens[2]);
                            createTimeDefined = true;
                            continue;
                        }
                        if (tokens[0].trim().startsWith("Name")) {
                            name = tokens[1];
                            nameDefined = true;
                            continue;
                        }
                        if (createTimeDefined && nameDefined) {
                            doHeader = false;
                        }
                        // Skip anything else
                        continue;
                    }
                    // Skip the column names line
                    if (tokens[0].trim().startsWith("time")) {
                        continue;
                    }
                    // Create the session
                    if (session == null) {
                        session = new Session(mDbAdapter, createTime);
                        session.setName(mDbAdapter, name);
                        sessionId = session.getId();
                        eventList = session.getEventList();
                        // Remove any events added in CTOR
                        for (Event event : eventList) {
                            session.removeEvent(mDbAdapter, event);
                        }
                    }
                    time = Long.parseLong(tokens[2]);
                    note = tokens[1];
                    session.addEvent(mDbAdapter, sessionId, time,
                            note);
                }
                // Check if ok
                if (session == null) {
                    Utils.errMsg(this, "Failed to the get necessary data to " +
                            "create a session");
                } else {
                    checkDuplicateSessionName(session);
                    Utils.infoMsg(this, "Created new session with:\n"
                            + "Name=" + session.getName() + "\n"
                            + "createTime="
                            + dateFormat.format(session.getCreateTime()) + "\n"
                            + "StartTime="
                            + dateFormat.format(session.getFirstEventTime()) + "\n"
                            + "EndTime="
                            + dateFormat.format(session.getEndTime()) + "\n"
                            + "Events=" + session.getEventList().size()
                    );
                }
            }
        } catch (Exception ex) {
            Utils.excMsg(this, "Got Exception reading CSV at line "
                    + lineNum, ex);
        }
        return session;
    }

    private void checkDuplicateSessionName(Session session) {
        if (session == null) return;
        Log.d(TAG, "checkDuplicateSessionName: " + session.getName());
        if (mSessionListAdapter == null) return;
        ArrayList<SessionDisplay> sessionList =
                mSessionListAdapter.getSessions();
        if (sessionList == null || sessionList.isEmpty()) {
            return;
        }
        String name = session.getName();
        for (SessionDisplay sessionDisplay : sessionList) {
            if (name.equals(sessionDisplay.getName())) {
                androidx.appcompat.app.AlertDialog.Builder builder =
                        new androidx.appcompat.app.AlertDialog.Builder(this);
                final EditText input = new EditText(this);
                if (session.getName() != null) {
                    input.setText(session.getName());
                }
                builder.setView(input);

                builder.setTitle("Session name already exists. Rename?");
                // Set up the buttons
                builder.setPositiveButton(android.R.string.ok,
                        (dialog, which) -> {
                            dialog.dismiss();
                            String newName = input.getText().toString();
                            boolean res = session.setName(mDbAdapter, newName);
                            if (!res) {
                                Utils.errMsg(this, "Error setting name: |"
                                        + newName + "|");
                            }
                            refresh();
                        });
                builder.setNegativeButton(android.R.string.cancel,
                        (dialog, which) -> dialog.cancel());
                builder.show();
            }
        }
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
     * Show the help.
     */
    private void showHelp() {
        Log.v(TAG, this.getClass().getSimpleName() + " showHelp");
        try {
            // Start the InfoActivity
            Intent intent = new Intent();
            intent.setClass(this, InfoActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            intent.putExtra(INFO_URL, "file:///android_asset/EventTimer.html");
            startActivity(intent);
        } catch (Exception ex) {
            Utils.excMsg(this, getString(R.string.help_show_error), ex);
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
    private void promptReplaceDatabase() {
        Log.d(TAG, "promptReplaceDatabase");
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
                                        Log.d(TAG, "Calling doReplaceDatabase" +
                                                ": " +
                                                "uri="
                                                + children.get(item).uri);
                                        doReplaceDatabase(children.get(item).uri);
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
    private void doReplaceDatabase(Uri uri) {
        Log.d(TAG, this.getClass().getSimpleName() + ": doReplaceDatabase: uri="
                + uri.getLastPathSegment());
        String lastSeg = uri.getLastPathSegment();
        if (!UriUtils.exists(this, uri)) {
            String msg = "Source database does not exist " + lastSeg;
            Log.d(TAG, "doReplaceDatabase: " + msg);
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
        refresh();
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