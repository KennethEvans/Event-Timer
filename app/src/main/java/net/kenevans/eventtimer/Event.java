package net.kenevans.eventtimer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Event implements IConstants {
    public static final SimpleDateFormat dateFormat =
            new SimpleDateFormat("E MMM d, yyyy HH:mm:ss", Locale.US);
    private long mDate = INVALID_DATE;
    private String mNote;

    public Event(long date, String note) {
        this.mDate = date;
        this.mNote = note;
    }

    @androidx.annotation.NonNull
    @Override
    public String toString() {
        String dateStr = (mDate != INVALID_DATE) ?
                dateFormat.format(new Date(mDate)) : "Undefined";
        String noteStr = (mNote != null) ? mNote : "";
        return "Time: " + dateStr + "\nNote: " + noteStr;
    }

    public long getDate() {
        return mDate;
    }

    public void setDate(long date) {
        this.mDate = date;
    }

    public String getNote() {
        return mNote;
    }

    public void setNote(String note) {
        this.mNote = note;
    }
}
