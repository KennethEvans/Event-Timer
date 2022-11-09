package net.kenevans.eventtimer;

import android.content.Intent;
import android.database.Cursor;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

// Adapter for holding sessions
public class SessionListAdapter extends BaseAdapter implements IConstants {
    public final ArrayList<SessionDisplay> mSessions;
    private final LayoutInflater mInflator;
    private final SessionsListActivity mActivity;
    private EventTimerDbAdapter mDbAdapter;

    public SessionListAdapter(SessionsListActivity activity,
                              EventTimerDbAdapter dbAdapter) {
        super();
        mActivity = activity;
        mDbAdapter = dbAdapter;

        mSessions = new ArrayList<>();
        mInflator = mActivity.getLayoutInflater();
        Cursor cursor = null;
        Cursor cursorEvents = null;
        int nItems = 0;
        try {
            if (mDbAdapter != null) {
                cursor = mDbAdapter.fetchAllSessionData(false);
                int indexId = cursor
                        .getColumnIndexOrThrow(COL_ID);
                int indexName = cursor
                        .getColumnIndexOrThrow(COL_NAME);
                int indexStart = cursor
                        .getColumnIndexOrThrow(COL_START_TIME);
                int indexEnd = cursor
                        .getColumnIndexOrThrow(COL_END_TIME);

                // Loop over items
                cursor.moveToFirst();
                long id;
                long startTime;
                long endTime;
                int nEvents = -1;
                String name;
                while (!cursor.isAfterLast()) {
                    nItems++;
                    id = cursor.getLong(indexId);
                    startTime = cursor.getLong(indexStart);
                    endTime = cursor.getLong(indexEnd);
                    name = cursor.getString(indexName);
                    // Get the event count
                    if (id != -1) {
                        cursorEvents =
                                mDbAdapter.fetchAllEventDataForSession(id);
                        if (cursorEvents != null) {
                            nEvents = cursorEvents.getCount();
                        }
                    }
//                    Log.d(TAG, "addSession: id=" + id + " startTime=" +
//                    startTime);
                    addSession(new SessionDisplay(id, name, startTime,
                            endTime, nEvents));
                    cursor.moveToNext();
                }
            }
        } catch (Exception ex) {
            Utils.excMsg(mActivity,
                    "Error getting sessions", ex);
        } finally {
            try {
                if (cursor != null) cursor.close();
                if (cursorEvents != null) cursorEvents.close();
            } catch (Exception ex) {
                // Do nothing
            }
        }
        Log.d(TAG, "Session list created with " + nItems + " items");
//        for (int i = 0; i < mSessions.size(); i++) {
//            SessionDisplay session = mSessions.get(i);
//            Log.d(TAG, "i=" + i + " id=" + session.getId());
//        }
    }


    public void addSession(SessionDisplay session) {
        if (!mSessions.contains(session)) {
            mSessions.add(session);
        }
    }

    public SessionDisplay getSession(int position) {
        return mSessions.get(position);
    }

    public void clear() {
        mSessions.clear();
    }

    @Override
    public int getCount() {
        return mSessions.size();
    }

    @Override
    public Object getItem(int i) {
        return mSessions.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
//        Log.d(TAG, "getView: i=" + i + " view=" + view);
        ViewHolder viewHolder;
        // General ListView optimization code.
        if (view == null) {
            view = mInflator.inflate(R.layout.listitem_session, viewGroup,
                    false);
            viewHolder = new ViewHolder();
            viewHolder.sessionCheckbox = view
                    .findViewById(R.id.session_checkbox);
            viewHolder.sessionInfo = view
                    .findViewById(R.id.session_info);
            view.setTag(viewHolder);

            viewHolder.sessionInfo.setOnClickListener(v -> {
//                Log.d(TAG, "sessionInfo.onClickListener");
                TextView tv = (TextView) v;
                SessionDisplay session =
                        (SessionDisplay) tv.getTag();
                long session_id = session.getId();
                Intent intent = new Intent(mActivity,
                        SessionActivity.class);
                intent.putExtra(SESSION_ID_CODE, session_id);
                mActivity.startActivity(intent);
//                Utils.infoMsg(mActivity, "sessionId=" + session.getId());
            });

            viewHolder.sessionCheckbox.setOnClickListener(v -> {
//                Log.d(TAG, "sessionCheckbox.onClickListener");
                CheckBox cb = (CheckBox) v;
                SessionDisplay session =
                        (SessionDisplay) cb.getTag();
                boolean checked = cb.isChecked();
                session.setChecked(checked);
            });
        } else {
//            Log.d(TAG, "tag=" + view.getTag());
            viewHolder = (ViewHolder) view.getTag();
        }

        SessionDisplay session = mSessions.get(i);
        // Set the name
        String sessionName = session.getName();
        if (viewHolder == null) {
            Log.d(TAG, "getView: viewHolder=null: "
                    + " id=" + view.getId()
                    + " view=" + view);
            return view;
        }
        Date startTime = new Date(session.getStartTime());
        String startStr = dateFormat.format(startTime);
        String endStr;
        if (session.getEndTime() == INVALID_TIME) {
            endStr = "NA";
        } else {
            Date endTime = new Date(session.getEndTime());
            endStr = dateFormat.format(endTime);
        }
        String nameStr = sessionName;
        if (sessionName == null || sessionName.length() == 0) {
            sessionName = startStr;
            nameStr = "Not Named";
        }
        int nEvents = session.getNEvents();
        String nEventsStr = "NA";
        if (nEvents != -1) {
            nEventsStr = String.format(Locale.US, "%d", nEvents);
        }
        String elapsedStr = "NA";
        if (startTime.getTime() != INVALID_TIME) {
            long elapsedTime;
            if (session.getEndTime() != INVALID_TIME) {
                Date endTime = new Date(session.getEndTime());
                elapsedTime = endTime.getTime() - startTime.getTime();
                elapsedStr =
                        DateUtils.formatElapsedTime(elapsedTime / 1000);
            }
        }
        String infoStr = String.format(Locale.US,
                "Start Time: %s\n"
                        + "EndTime: %s\n"
                        + "Events: %s\n"
                        + "Elapsed Time: %s\n"
                        + "Name: %s",
                startStr, endStr, nEventsStr, elapsedStr, nameStr);
        viewHolder.sessionCheckbox.setText(sessionName);
        viewHolder.sessionInfo.setText(infoStr);

        // Set the tag for the CheckBox and info to the session so we can
        // access which session in the onClickListener
        viewHolder.sessionInfo.setTag(session);
        viewHolder.sessionCheckbox.setTag(session);
        // And set the associated checkBox for the session
        session.setCheckBox(viewHolder.sessionCheckbox);
        viewHolder.sessionCheckbox.setChecked(session.isChecked());
        return view;
    }

    /**
     * Get a list of sessions.
     *
     * @return List of sessions.
     */
    public ArrayList<SessionDisplay> getSessions() {
        return mSessions;
    }

    /**
     * Get a list of checked sessions.
     *
     * @return List of sessions.
     */
    public ArrayList<SessionDisplay> getCheckedSessions() {
        ArrayList<SessionDisplay> checkedSessions = new ArrayList<>();
        for (SessionDisplay session : mSessions) {
            if (session.isChecked()) {
                checkedSessions.add(session);
            }
        }
        return checkedSessions;
    }


    /**
     * Convenience class for managing views for a ListView row.
     */
    static class ViewHolder {
        CheckBox sessionCheckbox;
        TextView sessionInfo;
    }
}
