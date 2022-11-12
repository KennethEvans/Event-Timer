package net.kenevans.eventtimer;

import android.widget.CheckBox;

public class SessionDisplay implements IConstants {
    private String name;
    private long id;
    private long startTime;
    private long endTime;
    private String duration;
    private boolean checked = false;
    private int nEvents;
    private CheckBox checkBox;

    public SessionDisplay(long id, String name, long startTime,
                          long endTime, int nEvents, String duration) {
        this.id = id;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.nEvents = nEvents;
        this.duration = duration;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    /**
     * Gets a name that is not null nor empty and which should be readable on
     * Windows.
     *
     * @return The safe name.
     */
    public String getSafeName() {
        if (name == null || name.length() == 0) {
            name = dateFormat.format(getStartTime());
        }
        // Remove illegal characters
        name = name.replaceAll("[^a-zA-Z0-9.\\-]", "_");
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getNEvents() {
        return nEvents;
    }

    public void setNEvents(int nEvents) {
        this.nEvents = nEvents;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public boolean isChecked() {
        return checked;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public CheckBox getCheckBox() {
        return checkBox;
    }

    public void setCheckBox(CheckBox checkBox) {
        this.checkBox = checkBox;
    }

}
