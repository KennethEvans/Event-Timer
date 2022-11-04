package net.kenevans.eventtimer;

import android.content.Context;
import android.text.format.DateUtils;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Session implements IConstants {
    public static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("E MMM d, yyyy HH:mm:ss", Locale.US);

    private long mStartTime = INVALID_DATE;
    private long mStopTime = INVALID_DATE;
    private String name = "";
    private List<Event> mEventList;

    public Session(long startTime, List<Event> eventList) {
        this.mStartTime = startTime;
        this.mEventList = eventList;
        eventList.add(new Event(startTime, "Start"));
    }

    @androidx.annotation.NonNull
    @Override
    public String toString() {
        Date now = new Date();
        Duration mElapsedTime;
        String nameStr = (name != null) ? name : "";
        String timeStr = dateFormat.format(now);
        String startStr = (mStartTime != INVALID_DATE) ?
                dateFormat.format(new Date(mStartTime)) : "";
        String stopStr = (mStopTime != INVALID_DATE) ?
                dateFormat.format(new Date(mStopTime)) : "";
        String elapsedStr;
        if (mStartTime != INVALID_DATE) {
            long elapsedTime;
            if (isStopped()) {
                elapsedTime = mStopTime - mStartTime;
            } else {
                elapsedTime = now.getTime() - mStartTime;
            }
            elapsedStr = DateUtils.formatElapsedTime(elapsedTime / 1000);
        } else {
            elapsedStr = "";
        }
        String nEventsStr = String.format(Locale.US, "%d", mEventList.size());

        return String.format("Time: %s\nStart Time: " +
                        "%s\nEndTime: %s\nEvents: %s\nElapsed Time: %s\n",
                timeStr, startStr, stopStr, nEventsStr, elapsedStr);

    }

    public void addEvent(Event event) {
        mEventList.add(event);
    }

    public void removeEvent(Context ctx, Event event) {
        try {
            mEventList.remove(event);
        } catch (Exception ex) {
            Utils.excMsg(ctx, "Error removing Event", ex);
        }
    }

    public void stop() {
        long now = new Date().getTime();
        mEventList.add(new Event(now, "Stop"));
        mStopTime = now;
    }

    public boolean isStopped() {
        return mStopTime != INVALID_DATE;
    }

    public long getStartTime() {
        return mStartTime;
    }

    public void setStartTime(long startTime) {
        this.mStartTime = startTime;
    }

    public long getStopTime() {
        return mStopTime;
    }

    public void setStopTime(long stopTime) {
        this.mStopTime = stopTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Event> getEventList() {
        return mEventList;
    }

    public void setEventList(List<Event> eventList) {
        this.mEventList = eventList;
    }
}
