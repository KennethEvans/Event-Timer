package net.kenevans.eventtimer;

import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * An Event has a time, a note, and the id of the session with which it is
 * associated.
 * <p>
 * An Event is stored in the database
 * in the events table with columns _id, time, note, and session_id.
 */
public class Event implements IConstants {
    public static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("E MMM d, yyyy HH:mm:ss", Locale.US);
    private long mTime;
    private String mNote;
    private long mId;
    private final long mSessionId;

//    /**
//     * CTOR that creates an Event and adds it to the associated Session in
//     * the database.
//     *
//     * @param dbAdapter The adapter.
//     * @param sessionId The session_id.
//     * @param time      The time.
//     * @param note      The note.
//     */
//    public Event(EventTimerDbAdapter dbAdapter, long sessionId, long time,
//                 String note) {
//        this.mSessionId = sessionId;
//        this.mTime = time;
//        this.mNote = note;
//        if (dbAdapter == null) {
//            Log.d(TAG, "Event CTOR got null adapter");
//            return;
//        }
//        Session.getSessionFromDb(dbAdapter, mSessionId)
//                .addEvent(dbAdapter, mSessionId, mTime, mNote);
//    }

    /**
     * CTOR that creates an event without storing it in the database. The
     * _id of its row in the database must be know.
     *
     * @param id        The _id.
     * @param sessionId The session_id. (_id of the Session.)
     * @param time      The time.
     * @param note      The note.
     */
    public Event(long id, long sessionId, long time, String note) {
        this.mSessionId = sessionId;
        this.mId = id;
        this.mTime = time;
        this.mNote = note;
    }

    @androidx.annotation.NonNull
    @Override
    public String toString() {
        String dateStr = (mTime != INVALID_TIME) ?
                dateFormat.format(new Date(mTime)) : "Undefined";
        String noteStr = (mNote != null) ? mNote : "";
        return "Time: " + dateStr + "\nNote: " + noteStr;
    }

    public long getTime() {
        return mTime;
    }

    public void removeEvent(EventTimerDbAdapter dbAdapter, Event event) {
        if (dbAdapter == null) {
            Log.d(TAG, "Event.removeEvent got null adapter");
            return;
        }
        Session.getSessionFromDb(dbAdapter, mSessionId)
                .removeEvent(dbAdapter, event);
    }

//    public void setTime(EventTimerDbAdapter dbAdapter, long sessionId,
//                        long time) {
//        this.mTime = time;
//        dbAdapter.updateEventTime(mId, mTime, sessionId);
//    }

    public String getNote() {
        return mNote;
    }

    public void setNote(EventTimerDbAdapter dbAdapter, String note) {
        this.mNote = note;
        dbAdapter.updateEventNote(mId, mNote);
    }

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        this.mId = id;
    }

    public long getSessionId() {
        return mSessionId;
    }
}
