package net.kenevans.eventtimer;

import android.util.Log;

/**
 * Class to extend Event to get a toString() that includes the elapsed time
 * from the previous event in the Session. Needs a dbAdapter to get the Session,
 * which is not available in Event.
 */
public class EventEx extends Event {
    private final Session mSession;

    public EventEx(EventTimerDbAdapter dbAdapter, Event event) {
        super(event.getId(), event.getSessionId(), event.getTime(),
                event.getNote());
        mSession = Session.getSessionFromDb(dbAdapter, event.getSessionId());
        if (mSession == null) {
            Log.d(TAG, "EventEx CTOR got null sessionId for event: id="
                    + event.getId() + " " + event);
        }
    }

    @androidx.annotation.NonNull
    @Override
    public String toString() {
        String info = super.toString();
        // Find the elapsed time from the previous session
        if (mSession == null) {
            return info;
        }
        String elapsedStr = "Elapsed Time: ";
        // Find the event with the same id
        Event event = null;
        // Find the position in the list
        int pos = -1;
        for (int i = 0; i < mSession.getEventList().size(); i++) {
            event = mSession.getEventList().get(i);
            if (event.getId() == getId()) {
                pos = i;
                break;
            }
        }
        if (pos == -1) {
            Log.d(TAG, "EventEx.toString() failed to find event="
                    + super.toString());
            return info;
        } else if (pos == 0) {
            elapsedStr += Utils.getDurationString(0, 0);
        } else {
            Event prev = mSession.getEventList().get(pos - 1);
            elapsedStr += Utils.getDurationString(prev.getTime(),
                    event.getTime());
        }
        info += "\n" + elapsedStr;
        return info;
    }
}
