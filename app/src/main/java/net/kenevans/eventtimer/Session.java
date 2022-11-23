package net.kenevans.eventtimer;

import android.database.Cursor;
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
 * Note that the start time returned by getCreateTime is the time the session
 * was created whereas the time returned by getFirstEventTime is the time of
 * the first event. These may be different.
 * <p>
 * A Session is stored in the database in the table session with columns
 * _id, create_time, and name. An Event is stored in the database
 * in the events table with columns _id, time, note, and session_id.
 */
public class Session implements IConstants {
    public static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("E MMM d, yyyy HH:mm:ss", Locale.US);

    private long mCreateTime;
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
     * @param useDb     Whether to store in the database or not.
     */
    public Session(EventTimerDbAdapter dbAdapter, long startTime,
                   boolean useDb) {
        this.mCreateTime = startTime;
        // Always add one event for the start
        List<Event> eventList = new ArrayList<>();
        if (useDb) {
            mId = dbAdapter.createSession(mCreateTime, mName);
            mEventList = eventList;
            addEvent(dbAdapter, mId, startTime, "Created");
        } else {
            // Use the startTime and an empty eventList
            this.mCreateTime = INVALID_TIME;
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
        if (sessionId < 0) {
            Log.d(TAG, "Session.getSessionFromDb got invalid sessionId="
                    + sessionId);
            return null;
        }
        Session session;
        try (Cursor cursor = dbAdapter.fetchSession(sessionId)) {
            if (cursor == null) return null;
            int indexStartTime = cursor
                    .getColumnIndex(COL_CREATE_TIME);
            int indexName = cursor.getColumnIndex(COL_NAME);

            // Loop over items
            long startTime = INVALID_TIME;
            String name = "";
            cursor.moveToFirst();
            if (indexStartTime != -1) {
                startTime = cursor.getLong(indexStartTime);
            }
            if (indexName != -1) {
                name = cursor.getString(indexName);
            }
            cursor.close();
            session = new Session(null, new Date().getTime(), false);
            session.mId = sessionId;
            session.mCreateTime = startTime;
            session.mName = name;
        } catch (Exception ex) {
            Log.d(TAG, "Failed to get Session from sessionId=" + sessionId, ex);
            return null;
        }

        // Get events
        try (Cursor cursorEvents =
                     dbAdapter.fetchAllEventDataForSession(sessionId)) {
            if (cursorEvents != null) {
                List<Event> eventList = new ArrayList<>();
                int indexId = cursorEvents.getColumnIndex(COL_ID);
                int indexTime = cursorEvents
                        .getColumnIndex(COL_TIME);
                int indexNote = cursorEvents.getColumnIndex(COL_NOTE);
                long id = -1;
                long time = INVALID_TIME;
                String note = "";
                cursorEvents.moveToFirst();
                while (!cursorEvents.isAfterLast()) {
                    if (indexId != -1) {
                        id = cursorEvents.getLong(indexId);
                    }
                    if (indexTime != -1) {
                        time = cursorEvents.getLong(indexTime);
                    }
                    if (indexNote != -1) {
                        note = cursorEvents.getString(indexNote);
                    }
                    eventList.add(new Event(id, sessionId, time, note));
                    cursorEvents.moveToNext();
                }
                cursorEvents.close();

                session.mEventList = eventList;
            }
        } catch (Exception ex) {
            Log.d(TAG, "Error getting events for sessionId=" + sessionId, ex);
            return null;
        }
        return session;
    }

    @androidx.annotation.NonNull
    @Override
    public String toString() {
        String nameStr = (mName != null) ? mName : "";
        String startStr = dateFormat.format(getFirstEventTime());
        String endStr = dateFormat.format(getEndTime());
        String durationStr = getDuration();
        String nEventsStr = String.format(Locale.US, "%d", mEventList.size());

        return String.format(Locale.US, "Start Time: %s\n"
                        + "EndTime: %s\nEvents: %s\nDuration: %s\n"
                        + "Name: %s",
                startStr, endStr, nEventsStr, durationStr, nameStr);
    }

    public void addEvent(EventTimerDbAdapter dbAdapter, long sessionId,
                         long time,
                         String note) {
        long newEventId = dbAdapter.createEvent(time, note, sessionId);
        Event event = new Event(newEventId, sessionId, time, note);
        mEventList.add(event);
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

    public String getDuration() {
        String durationStr;
        long startTime = getFirstEventTime();
        if (startTime != INVALID_TIME) {
            int size = mEventList.size();
            if (size < 2) {
                durationStr = Utils.getDurationString(0, 0);
            } else {
                durationStr = Utils.getDurationString(startTime,
                        mEventList.get(size - 1).getTime());
            }
        } else {
            durationStr = "NA";
        }
        return durationStr;
    }

    /**
     * Returns the time of the first event, not mCreateTime.
     *
     * @return The time of the first event.
     */
    public long getFirstEventTime() {
        long startTime = INVALID_TIME;
        if (mEventList.size() > 0) {
            startTime = mEventList.get(0).getTime();
        }
        return startTime;
    }

    /**
     * Returns mCreateTime, which is the time the Session was created, not
     * necessarily but usually the time of the first event.
     *
     * @return The time the session was created.
     */
    public long getCreateTime() {
        return mCreateTime;
    }

    public long getEndTime() {
        long endTime = INVALID_TIME;
        if (mEventList.size() > 0) {
            endTime = mEventList.get(mEventList.size() - 1).getTime();
        }
        return endTime;
    }

    public String getName() {
        return mName;
    }

    public boolean setName(EventTimerDbAdapter dbAdapter, String name) {
        this.mName = name;
        return dbAdapter.updateSessionName(mId, mName);
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
