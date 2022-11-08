package net.kenevans.eventtimer;

import android.database.Cursor;
import android.text.format.DateUtils;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Session contains a list of events.
 * <p>
 * An Event has a time, a note, and the id of the session with which it is
 * associated.
 * <p>
 * A session also has a start time and an end time. The end time is the time
 * of the last event or invalid there is none. The start time is the time the
 * session was created.
 * <p>
 * A Session is stored in the database in the table session with columns
 * _id,start_time, end_time, and name. An Event is stored in the database
 * in the events table with columns _id, time, note, and session_id.
 */
public class Session implements IConstants {
    public static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("E MMM d, yyyy HH:mm:ss", Locale.US);

    private long mStartTime;
    private long mStopTime = INVALID_TIME;
    private String mName = "";
    private List<Event> mEventList;
    private long mId = -1;

    /**
     * CTOR that uses the database.
     *
     * @param dbAdapter The adapter.
     * @param startTime The start time.
     */
    public Session(EventTimerDbAdapter dbAdapter, long startTime) {
        this(dbAdapter, startTime, true);
    }

    /**
     * CTOR that optionally uses the database.
     *
     * @param dbAdapter The adapter.
     * @param startTime The start time.
     * @param useDb Whether to store in the database or not.
     */
    public Session(EventTimerDbAdapter dbAdapter, long startTime,
                   boolean useDb) {
        this.mStartTime = startTime;
        List<Event> eventList = new ArrayList<>();
        if (useDb) {
            mId = dbAdapter.createSession(mStartTime, mStopTime, mName);
            mEventList = eventList;
            addEvent(dbAdapter, mId, startTime, "Start");
        } else {
            // Use the startTime and an empty eventList
            this.mStartTime = new Date().getTime();
            this.mEventList = eventList;
        }
    }

    /**
     * Get a complete Session form the database using its _id.
     *
     * @param dbAdapter The adapter.
     * @param sessionId The session id (_id).
     * @return The session.
     */
    public static Session getSessionFromDb(EventTimerDbAdapter dbAdapter,
                                           long sessionId) {
        Cursor cursor;
        try {
            cursor = dbAdapter.fetchSession(sessionId);
        } catch (Exception ex) {
            Log.d(TAG, "Failed to get Session from id=" + sessionId, ex);
            return null;
        }
        int indexStartTime = cursor
                .getColumnIndex(COL_START_TIME);
        int indexStopTime = cursor
                .getColumnIndex(COL_END_TIME);
        int indexName = cursor.getColumnIndex(COL_NAME);

        // Loop over items
        long startTime = INVALID_TIME;
        long stopTime = INVALID_TIME;
        String name = "";
        cursor.moveToFirst();
        if (indexStartTime != -1) {
            startTime = cursor.getLong(indexStartTime);
        }
        if (indexStopTime != -1) {
            stopTime = cursor.getLong(indexStopTime);
        }
        if (indexName != -1) {
            name = cursor.getString(indexName);
        }
        cursor.close();
        Session session = new Session(null, new Date().getTime(), false);
        session.mId = sessionId;
        session.mStartTime = startTime;
        session.mStopTime = stopTime;
        session.mName = name;

        // Get events
        cursor = dbAdapter.fetchAllEventDataForSession(sessionId);
        if (cursor != null) {
            List<Event> eventList = new ArrayList<>();
            int indexId = cursor.getColumnIndex(COL_ID);
            int indexTime = cursor
                    .getColumnIndex(COL_TIME);
            int indexNote = cursor.getColumnIndex(COL_NOTE);
            long id = -1;
            long time = INVALID_TIME;
            String note = "";
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                if (indexId != -1) {
                    id = cursor.getLong(indexId);
                }
                if (indexTime != -1) {
                    time = cursor.getLong(indexStartTime);
                }
                if (indexNote != -1) {
                    note = cursor.getString(indexNote);
                }
                eventList.add(new Event(id, sessionId, time, note));
                cursor.moveToNext();
            }
            cursor.close();

            session.mEventList = eventList;
        }
        return session;
    }

    @androidx.annotation.NonNull
    @Override
    public String toString() {
        String nameStr = (mName != null) ? mName : "";
//        String timeStr = dateFormat.format(now);
        String startStr = (mStartTime != INVALID_TIME) ?
                dateFormat.format(new Date(mStartTime)) : "";
        String stopStr = (mStopTime != INVALID_TIME) ?
                dateFormat.format(new Date(mStopTime)) : "";
        String elapsedStr;
        if (mStartTime != INVALID_TIME) {
            long elapsedTime;
            int size = mEventList.size();
            if (size < 2) {
                elapsedTime = 0;
            } else {
                elapsedTime = mEventList.get(size - 1).getTime() - mStartTime;
            }
            elapsedStr = DateUtils.formatElapsedTime(elapsedTime / 1000);
        } else {
            elapsedStr = "";
        }
        String nEventsStr = String.format(Locale.US, "%d", mEventList.size());

        return String.format(Locale.US, "Start Time: %s\n"
                        + "EndTime: %s\nEvents: %s\nElapsed Time: %s\n"
                        + "Name: %s",
                startStr, stopStr, nEventsStr, elapsedStr, nameStr);
    }

    public void addEvent(EventTimerDbAdapter dbAdapter, long sessionId,
                         long time,
                         String note) {
        long newEventId = dbAdapter.createEvent(time, note, sessionId);
        Event event = new Event(newEventId, sessionId, time, note);
        mEventList.add(event);
        setStopTime(dbAdapter, time);
    }

    public boolean removeEvent(EventTimerDbAdapter dbAdapter, Event event) {
        boolean retVal = true;
        try {
            dbAdapter.deleteEvent(event.getId(), event.getSessionId());
            mEventList.remove(event);

        } catch (Exception ex) {
            retVal = false;
        }
        return retVal;
    }

    public long getStartTime() {
        return mStartTime;
    }

    public void setStartTime(EventTimerDbAdapter dbAdapter, long startTime) {
        this.mStartTime = startTime;
        dbAdapter.updateSessionStartTime(mId, mStartTime);
    }

    public long getStopTime() {
        return mStopTime;
    }

    public void setStopTime(EventTimerDbAdapter dbAdapter, long stopTime) {
        this.mStopTime = stopTime;
        dbAdapter.updateSessionStopTime(mId, mStopTime);
    }

    public String getName() {
        return mName;
    }

    public void setName(EventTimerDbAdapter dbAdapter, String name) {
        this.mName = name;
        dbAdapter.updateSessionName(mId, mName);
    }

    public List<Event> getEventList() {
        return mEventList;
    }

    public void setEventList(List<Event> eventList) {
        this.mEventList = eventList;
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        this.mId = id;
    }
}
