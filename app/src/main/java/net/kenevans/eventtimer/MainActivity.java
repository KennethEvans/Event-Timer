package net.kenevans.eventtimer;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity implements IConstants {
    public static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("E MMM d, yyyy HH:mm:ss", Locale.US);
    TextView mTextViewEvent;
    private ListView mListView;
    private Menu mMenu;
    private Event mCurrentEvent;
    private Button mButtonStart;
    private Button mButtonStop;
    private Button mButtonRecord;

    private Handler mHandler = new Handler();
    private boolean mTimerStarted;
    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            try{
                updateInfo();
                resetListView();
            }
            catch (Exception ex) {
                Utils.excMsg(MainActivity.this, "Error in timer", ex);
            }
            finally{
                //also call the same runnable to call it at regular interval
                mHandler.postDelayed(this, 1000);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.v(TAG, this.getClass().getSimpleName() + " onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextViewEvent = findViewById(R.id.event);
        mListView = findViewById(R.id.mainListView);

        mButtonStart = findViewById(R.id.buttonStart);
        mButtonStart.setOnClickListener(v -> start());
        mButtonStop = findViewById(R.id.buttonStop);
        mButtonStop.setOnClickListener(v -> stop());
        mButtonRecord = findViewById(R.id.buttonRecord);
        mButtonRecord.setOnClickListener(v -> record());


        List<EventData> eventList  = new ArrayList<>();
        mCurrentEvent = new Event(new Date().getTime(), eventList);
        startTimer();


        // Ask for needed permissions
        requestPermissions();
    }

    @Override
    protected void onPause() {
        Log.v(TAG, this.getClass().getSimpleName() + " onPause");
        super.onPause();

    }

    @Override
    public void onResume() {
        Log.d(TAG, this.getClass().getSimpleName() + " onResume:");
        super.onResume();
        updateInfo();

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
    public boolean onCreateOptionsMenu(Menu menu) {
//        Log.d(TAG, this.getClass().getSimpleName() + " onCreateOptionsMenu");
//        Log.d(TAG, "    mPlaying=" + mPlaying);
        mMenu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
//        if (mApi == null) {
//            mMenu.findItem(R.id.pause).setTitle("Start");
//            mMenu.findItem(R.id.save).setVisible(false);
//        } else if (mPlaying) {
//            mMenu.findItem(R.id.pause).setIcon(ResourcesCompat.
//                    getDrawable(getResources(),
//                            R.drawable.ic_stop_white_36dp, null));
//            mMenu.findItem(R.id.pause).setTitle("Pause");
//            mMenu.findItem(R.id.save).setVisible(false);
//        } else {
//            mMenu.findItem(R.id.pause).setIcon(ResourcesCompat.
//                    getDrawable(getResources(),
//                            R.drawable.ic_play_arrow_white_36dp, null));
//            mMenu.findItem(R.id.pause).setTitle("Start");
//            mMenu.findItem(R.id.save).setVisible(true);
//        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
//        if (id == R.id.pause) {
//            if (mApi == null) {
//                return true;
//            }
//            if (mPlaying) {
//                // Turn it off
//                setLastHr();
//                mStopTime = new Date();
//                mPlaying = false;
//                setPanBehavior();
//                if (mEcgDisposable != null) {
//                    // Turns it off
//                    streamECG();
//                }
//                mMenu.findItem(R.id.pause).setIcon(ResourcesCompat.
//                        getDrawable(getResources(),
//                                R.drawable.ic_play_arrow_white_36dp, null));
//                mMenu.findItem(R.id.pause).setTitle("Start");
//                mMenu.findItem(R.id.save).setVisible(true);
//            } else {
//                // Turn it on
//                setLastHr();
//                mStopTime = new Date();
//                mPlaying = true;
//                setPanBehavior();
//                mTextViewTime.setText(getString(R.string.elapsed_time,
//                        0.0));
//                // Clear the plot
//                mECGPlotter.clear();
//                mQRSPlotter.clear();
//                mHRPlotter.clear();
//                if (mEcgDisposable == null) {
//                    // Turns it on
//                    streamECG();
//                }
//                mMenu.findItem(R.id.pause).setIcon(ResourcesCompat.
//                        getDrawable(getResources(),
//                                R.drawable.ic_stop_white_36dp, null));
//                mMenu.findItem(R.id.pause).setTitle("Pause");
//                mMenu.findItem(R.id.save).setVisible(false);
//            }
//            return true;
//        } else if (id == R.id.save_plot) {
//            saveDataWithNote(SaveType.PLOT);
//            return true;
//        } else if (id == R.id.save_data) {
//            saveDataWithNote(SaveType.DATA);
//            return true;
//        } else if (id == R.id.save_both) {
//            saveDataWithNote(SaveType.BOTH);
//            return true;
//        } else if (id == R.id.save_all) {
//            saveDataWithNote(SaveType.ALL);
//            return true;
//        } else if (id == R.id.save_device_data) {
//            doSaveSessionData(SaveType.DEVICE_HR);
//            return true;
//        } else if (id == R.id.save_qrs_data) {
//            doSaveSessionData(SaveType.QRS_HR);
//            return true;
//        } else if (id == R.id.info) {
//            info();
//            return true;
//        } else if (id == R.id.restart_api) {
//            restartApi();
//            return true;
//        } else if (id == R.id.redo_plot_setup) {
//            redoPlotSetup();
//            return true;
//        } else if (id == R.id.device_id) {
//            selectDeviceId();
//            return true;
//        } else if (id == R.id.choose_data_directory) {
//            chooseDataDirectory();
//            return true;
//        } else if (id == R.id.help) {
//            showHelp();
//            return true;
//        } else if (item.getItemId() == R.id.menu_settings) {
//            showSettings();
//            return true;
//        }
        return false;
    }

    private void start() {
        if(mCurrentEvent != null && !mCurrentEvent.isStopped()) {
            mCurrentEvent.stop();
            stopTimer();
        }
        List<EventData> eventList  = new ArrayList<>();
        mCurrentEvent = new Event(new Date().getTime(), eventList);
        resetListView();
        startTimer();
    }

    private void stop() {
        if(mCurrentEvent == null) {
            Utils.errMsg(this, "There is no current event");
            return;
        }
        if(mCurrentEvent.isStopped()) {
            Utils.errMsg(this, "The current event is already stopped");
            return;
        }
        mCurrentEvent.stop();
        resetListView();
        stopTimer();
    }

    private void record() {
        if(mCurrentEvent == null) {
            Utils.errMsg(this, "There is no current event");
            return;
        }
        if(mCurrentEvent.isStopped()) {
            Utils.errMsg(this, "The current event is stopped");
            return;
        }
        mCurrentEvent.eventList.add(new EventData(new Date().getTime(),
                "Event " + mCurrentEvent.eventList.size()));
        resetListView();
    }

    private void startTimer() {
        mHandler.post(mRunnable);
        mTimerStarted = true;
    }

    private void stopTimer() {
        if(!mTimerStarted ) {
            return;
        }
        mHandler.removeCallbacks(mRunnable);
        mTimerStarted = false;
    }

    private void updateInfo() {
        if(mCurrentEvent != null) {
            Log.d(TAG, this.getClass().getSimpleName() + ": updateInfo: "
                    + "nEvents=" + mCurrentEvent.eventList.size());
        }
        String infoStr = "";
        if( mCurrentEvent != null) {
            infoStr = mCurrentEvent.toString();
        }
         mTextViewEvent.setText(infoStr);
    }

    /**
     * Resets the file list.
     */
    private void resetListView() {
//        Log.d(TAG, this.getClass().getSimpleName() + ": resetListView: "
//                + "mListView=" + mListView);
        if(mCurrentEvent == null || mCurrentEvent.eventList == null) {
            return;
        }

        // Set the ListAdapter
        ArrayAdapter<EventData> fileList = new ArrayAdapter<>(this,
                R.layout.row, mCurrentEvent.eventList);
        mListView.setAdapter(fileList);
        mListView.setSelection(mListView.getCount() - 1);

        mListView.setOnItemClickListener((parent, view, pos, id) -> {
            if (pos < 0 || pos >= mCurrentEvent.eventList.size()) {
                return;
            }
//            // Create the result Intent and include the fileName
//            Intent intent = new Intent();
//            intent.putExtra(EXTRA_IMAGE_URI,
//                    mUriList.get(pos).uri.toString());
//            // Set result and finish this Activity
//            setResult(Activity.RESULT_OK, intent);
//            finish();
        });
    }

    public void requestPermissions() {
        Log.d(TAG, "requestPermissions");
//        BluetoothManager bluetoothManager =
//                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        if (bluetoothManager != null) {
//            BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
//            if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
//                Intent enableBtIntent =
//                        new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//                enableBluetoothLauncher.launch(enableBtIntent);
//            }
//        }
//
//        if (Build.VERSION.SDK_INT >= 31) {
//            // Android 12 (S)
//            this.requestPermissions(new String[]{
//                            Manifest.permission.BLUETOOTH_SCAN,
//                            Manifest.permission.BLUETOOTH_CONNECT},
//                    REQ_ACCESS_PERMISSIONS);
//        } else {
//            // Android 6 (M)
//            this.requestPermissions(new String[]{
//                            Manifest.permission.ACCESS_FINE_LOCATION},
//                    REQ_ACCESS_PERMISSIONS);
//        }

    }

}