package net.kenevans.eventtimer;

import android.text.format.DateUtils;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class Event implements IConstants {
    public static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("E MMM d, yyyy HH:mm:ss", Locale.US);

    public long startTime = INVALID_DATE;
    public long stopTime = INVALID_DATE;
    String name = "";
    public List<EventData> eventList;

    public Event(long startTime, List<EventData> eventList) {
        this.startTime = startTime;
        this.eventList = eventList;
        eventList.add(new EventData(startTime, "Start"));
    }

    @androidx.annotation.NonNull
    @Override
    public String toString() {
        Date now = new Date();
        Duration mElapsedTime;
        String nameStr = (name != null) ? name : "";
        String timeStr = dateFormat.format(now);
        String startStr = (startTime != INVALID_DATE) ?
                dateFormat.format(new Date(startTime)) : "";
        String stopStr = (stopTime != INVALID_DATE) ?
                dateFormat.format(new Date(stopTime)) : "";
        String elapsedStr;
        if (startTime != INVALID_DATE) {
            long elapsedTime;
            if (isStopped()) {
                elapsedTime = stopTime - startTime;
            } else {
                elapsedTime = now.getTime() - startTime;
            }
            elapsedStr = DateUtils.formatElapsedTime(elapsedTime / 1000);
        } else {
            elapsedStr = "";
        }
        String nEventsStr = String.format(Locale.US, "%d", eventList.size());

        return String.format("Time: %s\nStart Time: " +
                        "%s\nEndTime: %s\nEvents: %s\nElapsed Time: %s\n",
                timeStr, startStr, stopStr, nEventsStr, elapsedStr);

    }

    public void stop() {
        long now = new Date().getTime();
        eventList.add(new EventData(now, "Stop"));
        stopTime = now;
    }

    public boolean isStopped() {
        return stopTime != INVALID_DATE;
    }

}
