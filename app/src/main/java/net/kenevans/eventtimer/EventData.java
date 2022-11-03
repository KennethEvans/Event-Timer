package net.kenevans.eventtimer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EventData implements IConstants {
    public static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("E MMM d, yyyy HH:mm:ss", Locale.US);
    public long date = INVALID_DATE;
    public String note;

    public EventData(long date, String note) {
        this.date = date;
        this.note = note;
    }

    @androidx.annotation.NonNull
    @Override
    public String toString() {
        String dateStr = (date != INVALID_DATE) ?
                dateFormat.format(new Date(date)) : "Undefined";
        String noteStr = (note != null) ? note : "";
        return "Time: " + dateStr + "\nNote: " + noteStr;
    }

}
