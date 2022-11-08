package net.kenevans.eventtimer;

import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Date;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class SessionsListActivity extends AppCompatActivity implements IConstants {
    //    private final List<Session> mSessionList = new ArrayList<>();
    ListView mListView;
    private EventTimerDbAdapter mDbAdapter;
    private SessionListAdapter mSessionListAdapter;

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
        fab.setOnClickListener(view -> {
            Log.d(TAG, "FloatingActionButton onClick nSessions="
                    + mSessionListAdapter.mSessions.size()
                    + " thread=" + Thread.currentThread());
            addEvent(null);
        });

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

    private void addEvent(String name) {
        long now = new Date().getTime();
        // Add at beginning
        Session session = new Session(mDbAdapter, now);
        if(name != null) {
            session.setName(mDbAdapter, name);
        }
        mSessionListAdapter.addSession(new SessionDisplay(session.getId(),
                session.getName(), session.getStartTime(),
                session.getStopTime(), session.getEventList().size()));

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
}