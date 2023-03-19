package net.kenevans.eventtimer;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class SessionActivity extends AppCompatActivity implements IConstants {
    TextView mTextViewEvent;
    TextView mTextViewTime;
    private ListView mListView;
    private Session mCurrentSession;
    private EventTimerDbAdapter mDbAdapter;

    // Set up the timer
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean mTimerStarted;
    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                updateInfo();
//                resetListView();
            } catch (Exception ex) {
                Utils.excMsg(SessionActivity.this, "Error in timer", ex);
            } finally {
                //also call the same runnable to call it at regular interval
                mHandler.postDelayed(this, 1000);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, this.getClass().getSimpleName() + " onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session);
        mTextViewTime = findViewById(R.id.time);
        mTextViewEvent = findViewById(R.id.event);
        mListView = findViewById(R.id.sessionListView);

        Button mButtonStart = findViewById(R.id.buttonStart);
        mButtonStart.setOnClickListener(v -> start());
        Button mButtonStop = findViewById(R.id.buttonStop);
        mButtonStop.setOnClickListener(v -> stop());
        Button mButtonRecord = findViewById(R.id.buttonRecord);
        mButtonRecord.setOnClickListener(v -> record());

        mTextViewEvent.setOnClickListener(v -> renameSession(mCurrentSession));

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Keep the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mDbAdapter = new EventTimerDbAdapter(this);
        mDbAdapter.open();

        Bundle extras = getIntent().getExtras();
        long sessionId;

        if (extras != null) {
            sessionId = extras.getLong(SESSION_ID_CODE, -1);
            if (sessionId < 0) {
                Utils.errMsg(this, "SessionActivity received an invalid " +
                        "session");
            } else {
                mCurrentSession = Session.getSessionFromDb(mDbAdapter,
                        sessionId);
            }
        } else {
            Utils.errMsg(this, "SessionActivity did not get a valid Session");
            mCurrentSession = null;
        }

        if (getSupportActionBar() != null) {
            String title = "Current Session";
            if (mCurrentSession != null && mCurrentSession.getName() != null
                    && mCurrentSession.getName().length() > 0) {
                title = mCurrentSession.getName();
            }
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(title);
        }

        // Decide whether to start the timer
        SharedPreferences prefs = getSharedPreferences(MAIN_ACTIVITY,
                MODE_PRIVATE);
        boolean startTimer = prefs.getBoolean(PREF_START_TIMER_INITIALLY,
                false);
        if (startTimer) {
            startTimer();
        }
        Log.d(TAG, this.getClass().getSimpleName() + " onCreate:"
                + " mTimerStarted=" + mTimerStarted
                + " startTimer=" + startTimer);

        // Ask for needed permissions
        requestPermissions();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, this.getClass().getSimpleName() + " onPause"
                + " mTimerStarted=" + mTimerStarted);
        super.onPause();
        SharedPreferences.Editor editor =
                getSharedPreferences(MAIN_ACTIVITY, MODE_PRIVATE).edit();
        editor.putBoolean(PREF_START_TIMER_INITIALLY, mTimerStarted);
        editor.apply();
    }

    @Override
    public void onResume() {
        Log.d(TAG, this.getClass().getSimpleName() + " onResume:"
                + " mTimerStarted=" + mTimerStarted);
        super.onResume();
        SharedPreferences.Editor editor =
                getSharedPreferences(MAIN_ACTIVITY, MODE_PRIVATE).edit();
        editor.putBoolean(PREF_START_TIMER_INITIALLY, mTimerStarted);
        editor.apply();
        resetListView();
        updateInfo();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        Log.d(TAG, this.getClass().getSimpleName() + " onCreateOptionsMenu");
//        Log.d(TAG, "    mPlaying=" + mPlaying);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_session, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (id == R.id.menu_custom_event) {
            promptForCustomEvent();
            return true;
        }
        return false;
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            record();
            return true;
        }
        return false;
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

    private void start() {
        Log.d(TAG, this.getClass().getSimpleName() + " start");
        if (mCurrentSession == null) {
            return;
        }
        if (!mTimerStarted) {
            stopTimer();
        }
        startTimer();
        updateInfo();
    }

    private void stop() {
        Log.d(TAG, this.getClass().getSimpleName() + " stop");
        if (mCurrentSession == null) {
            Utils.errMsg(this, "There is no current event");
            return;
        }
        stopTimer();
        updateInfo();
    }

    private void record() {
        if (mCurrentSession == null) {
            Utils.errMsg(this, "There is no current session");
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input  = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        if (mCurrentSession.getName() != null) {
            input.setText("");
        }
        builder.setView(input);

        builder.setTitle("Enter the note");
        // Set up the buttons
        builder.setPositiveButton(android.R.string.ok,
                (dialog, which) -> {
                    dialog.dismiss();
                    String note = input.getText().toString();
                    long now = new Date().getTime();
                    mCurrentSession.addEvent(mDbAdapter,
                            mCurrentSession.getId(), now, note);
                    mCurrentSession = Session.getSessionFromDb(mDbAdapter,
                            mCurrentSession.getId());
                    updateInfo();
                    resetListView();
                });
        builder.setNegativeButton(android.R.string.cancel,
                (dialog, which) -> dialog.cancel());
        AlertDialog alertDialog = builder.create();
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                return true;
            }
            return false;
        });
        alertDialog.show();
    }

    private void startTimer() {
        mHandler.post(mRunnable);
        mTimerStarted = true;
    }

    private void stopTimer() {
        if (!mTimerStarted) {
            return;
        }
        mHandler.removeCallbacks(mRunnable);
        mTimerStarted = false;
    }

    /**
     * Updates the info part of the screen.
     */
    private void updateInfo() {
//        if(mCurrentSession != null) {
//            Log.d(TAG, this.getClass().getSimpleName() + ": updateInfo: "
//                    + "nEvents=" + mCurrentSession.eventList.size());
//        }
        // Add the time then get the Session data
        if (mTimerStarted) {
            Date now = new Date();
            mTextViewTime.setText(dateFormatAmPm.format(now));
        } else {
            mTextViewTime.setText(R.string.not_running_label);
        }
        String infoStr = "";
        if (mCurrentSession != null) {
            infoStr += mCurrentSession.toString();
        } else {
            infoStr += "No session";
        }
        mTextViewEvent.setText(infoStr);
    }

    private void promptForCustomEvent() {
        final Calendar date = Calendar.getInstance();
        new DatePickerDialog(this, (view, year, monthOfYear, dayOfMonth) -> {
            date.set(year, monthOfYear, dayOfMonth);
            new TimePickerDialog(SessionActivity.this,
                    (view1, hourOfDay, minute) -> {
                        date.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        date.set(Calendar.MINUTE, minute);
                        date.set(Calendar.SECOND, 0);
                        date.set(Calendar.MILLISECOND, 0);
                        setNewCustomEvent(date);
                    }, date.get(Calendar.HOUR_OF_DAY),
                    date.get(Calendar.MINUTE), false).show();
        }, date.get(Calendar.YEAR), date.get(Calendar.MONTH),
                date.get(Calendar.DATE)).show();
    }


    private void setNewCustomEvent(Calendar cal) {
        Log.d(TAG, this.getClass().getSimpleName() + " setEventNote");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
            input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        builder.setView(input);
        builder.setTitle("Enter the new note");
        // Set up the buttons
        builder.setPositiveButton(android.R.string.ok,
                (dialog, which) -> {
                    dialog.dismiss();
                    String note = input.getText().toString();
                    mCurrentSession.addEvent(mDbAdapter,
                            mCurrentSession.getId(), cal.getTimeInMillis(),
                            note);
                    mCurrentSession = Session.getSessionFromDb(mDbAdapter,
                            mCurrentSession.getId());
                    updateInfo();
                    resetListView();
                });
        builder.setNegativeButton(android.R.string.cancel,
                (dialog, which) -> dialog.cancel());
        AlertDialog alertDialog = builder.create();
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                return true;
            }
            return false;
        });
        alertDialog.show();
    }

    private void setEventNote(Event event) {
        Log.d(TAG, this.getClass().getSimpleName() + " setEventNote");
        if (event == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        if (event.getNote() != null) {
            input.setText(event.getNote());
        }
        builder.setView(input);
        builder.setTitle("Enter the new note");
        // Set up the buttons
        builder.setPositiveButton(android.R.string.ok,
                (dialog, which) -> {
                    dialog.dismiss();
                    String note = input.getText().toString();
                    boolean res = event.setNote(mDbAdapter, note);
                    mCurrentSession = Session.getSessionFromDb(mDbAdapter,
                            mCurrentSession.getId());
                    if (!res) {
                        Utils.errMsg(this, "Error setting note: |"
                                + note + "|");
                    }
                    resetListView();
                });
        builder.setNegativeButton(android.R.string.cancel,
                (dialog, which) -> dialog.cancel());
        AlertDialog alertDialog = builder.create();
        input.setOnEditorActionListener((v, actionId, event1) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                return true;
            }
            return false;
        });
        alertDialog.show();
    }

    private void renameSession(Session session) {
        Log.d(TAG, this.getClass().getSimpleName() + " setEventNote");
        if (session == null) return;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        if (session.getName() != null) {
            input.setText(session.getName());
        }
        builder.setView(input);

        builder.setTitle("Enter the new name");
        // Set up the buttons
        builder.setPositiveButton(android.R.string.ok,
                (dialog, which) -> {
                    dialog.dismiss();
                    String name = input.getText().toString();
                    boolean res = session.setName(mDbAdapter, name);
                    if (!res) {
                        Utils.errMsg(this, "Error setting name: |"
                                + name + "|");
                    }
                    resetListView();
                    // Reset the title
                    if (getSupportActionBar() != null) {
                        String title = "Current Session";
                        if (mCurrentSession != null && mCurrentSession.getName() != null
                                && mCurrentSession.getName().length() > 0) {
                            title = mCurrentSession.getName();
                        }
                        getSupportActionBar().setTitle(title);
                    }

                });
        builder.setNegativeButton(android.R.string.cancel,
                (dialog, which) -> dialog.cancel());
        AlertDialog alertDialog = builder.create();
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();
                return true;
            }
            return false;
        });
        alertDialog.show();
    }

    /***
     * Deletes the given Event.
     *
     * @param event The event.
     * @return True if successful else return false.
     */
    private boolean deleteEventData(Event event) {
        Log.d(TAG, this.getClass().getSimpleName() + " deleteEventData");
        if (event == null) return false;
        boolean retVal = mCurrentSession.removeEvent(mDbAdapter, event);
        mCurrentSession = Session.getSessionFromDb(mDbAdapter,
                mCurrentSession.getId());
        updateInfo();
        resetListView();
        return retVal;
    }

    /**
     * Resets the file list.
     */
    @SuppressWarnings("unchecked")
    private void resetListView() {
        if (mCurrentSession == null) {
            Log.d(TAG, this.getClass().getSimpleName() + ": resetListView: "
                    + "mCurrentSession=null");
        } else {
            Log.d(TAG, this.getClass().getSimpleName() + ": resetListView: "
                    + "nEvents=" + mCurrentSession.getEventList().size());
        }
        if (mCurrentSession == null) {
            Log.d(TAG, "resetListView: mCurrentSession is null");
            ArrayAdapter<EventEx> adapter =
                    (ArrayAdapter<EventEx>) mListView.getAdapter();
            if (adapter != null) adapter.clear();
            return;
        }
        if (mCurrentSession.getEventList() == null) {
            Log.d(TAG, "resetListView: mCurrentSession.getEventList() is null");
            ArrayAdapter<EventEx> adapter =
                    (ArrayAdapter<EventEx>) mListView.getAdapter();
            if (adapter != null) adapter.clear();
            return;
        }

        // Get the eventList as a copy and reverse it
        List<EventEx> eventListEx = new ArrayList<>();
        int nEvents = mCurrentSession.getEventList().size();
        if (nEvents > 0) {
            Event event;
            for (int i = nEvents - 1; i >= 0; i--) {
                event = mCurrentSession.getEventList().get(i);
                eventListEx.add(new EventEx(mDbAdapter, event));
            }
        }

        // Set the ListAdapter
        ArrayAdapter<EventEx> adapter = new ArrayAdapter<>(this,
                R.layout.row, eventListEx);
        mListView.setAdapter(adapter);

        mListView.setOnItemClickListener((parent, view, pos, id) -> {
            if (pos < 0 || pos >= eventListEx.size()) {
                return;
            }
            EventEx selectedEvent =
                    (EventEx) parent.getItemAtPosition(pos);
            int checkedItem = 0;
            String[] items = {"Edit Note", "Delete"};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Pick an operation");
            builder.setSingleChoiceItems(items, checkedItem,
                    (dialogInterface, which) -> {
                        if (which == 0) {
                            setEventNote(selectedEvent);
                        } else if (which == 1) {
                            boolean res = deleteEventData(selectedEvent);
                            if (!res) {
                                Utils.errMsg(this, "Failed to remove the " +
                                        "event");
                            }
                        }
                        dialogInterface.dismiss();
                        resetListView();
                    });
            builder.setNegativeButton(R.string.cancel,
                    (dialogInterface1, which1) -> dialogInterface1.dismiss());
            AlertDialog alert = builder.create();
            alert.setCanceledOnTouchOutside(false);
            alert.show();

        });
    }

    public void requestPermissions() {
        Log.d(TAG, "requestPermissions");
    }
}