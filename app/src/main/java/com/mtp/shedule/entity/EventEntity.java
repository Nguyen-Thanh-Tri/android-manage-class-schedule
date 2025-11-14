package com.mtp.shedule.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.mtp.shedule.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Entity(tableName = "events")
public class EventEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String description;
    public long startTime;   // millis
    public long endTime;     // millis
    public String timezone;
    public String repeat;    // "None", "Daily", "Weekly"...

    public int color;        // lưu index màu card

    public EventEntity(String title, String description, long startTime, long endTime, String timezone, String repeat) {
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.timezone = timezone;
        this.repeat = repeat;
        this.color = R.drawable.gradient_bg_red;
    }
    public int getId() { return id; }

    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }

    public void setDescription(String description) { this.description = description; }
    public long getStartTime() { return startTime; }

    public void setStartTime(long startTime) { this.startTime = startTime; }
    public long getEndTime() { return endTime; }

    public void setEndTime(long endTime) { this.endTime = endTime; }
    public String getTimezone() { return timezone; }

    public void setTimezone(String timezone) { this.timezone = timezone; }
    public String getRepeat() { return repeat; }
    public void setRepeat(String repeat) { this.repeat = repeat; }
    public int getColor() { return color; }

    public void setColor(int color) { this.color = color; }
    public String getStartTimeFormatted() {
        return formatTime(startTime);
    }

    public String getEndTimeFormatted() {
        return formatTime(endTime);
    }

    private String formatTime(long timeMillis) {
        // Định dạng HH:mm
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timeMillis));
    }
}

