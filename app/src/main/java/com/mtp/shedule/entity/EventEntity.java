package com.mtp.shedule.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import com.mtp.shedule.R;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@Entity(tableName = "events")
public class EventEntity implements Serializable {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String title;
    public String description;
    public long startTime;   // millis
    public long endTime;     // millis

    public int color;        // lưu index màu card
    
    // Repeat functionality
    public String repeatType;    // "none", "weekly", "daily", "monthly"
    public String dayOfWeek;     // for weekly repeats ("monday", "tuesday", etc.)
    @ColumnInfo(name = "reminder")
    public int reminder = 0;
    
    // Course-related fields (optional, for course-type events)
    public String teacher;       // teacher name for course events
    public String room;          // room location for course events
    public boolean isCourse;     // flag to identify course events

    // Default constructor for Room
    public EventEntity() {
        this.color = 0;
        this.repeatType = "none";
        this.isCourse = false;
        this.reminder = 0;
    }

    @Ignore
    public EventEntity(String title, String description, long startTime, long endTime) {
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.color = 0;
        this.repeatType = "none";
        this.isCourse = false;
        this.reminder = 0;
    }
    
    // Constructor for course events
    @Ignore
    public EventEntity(String title, String teacher, String room, String dayOfWeek, long startTime, long endTime) {
        this.title = title;
        this.teacher = teacher;
        this.room = room;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.repeatType = "weekly";
        this.isCourse = true;
        this.description = "Room: " + room + "\nTeacher: " + teacher;
        this.color = 0;
        this.reminder = 0;
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
//    public String getTimezone() { return timezone; }
    public int getReminder() { return reminder; }
    public void setReminder(int reminder) { this.reminder = reminder; }
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
    
    public String getRepeatType() { return repeatType; }
    public void setRepeatType(String repeatType) { this.repeatType = repeatType; }
    
    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
    
    public String getTeacher() { return teacher; }
    public void setTeacher(String teacher) { this.teacher = teacher; }
    
    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }
    
    public boolean isCourse() { return isCourse; }
    public void setIsCourse(boolean isCourse) { this.isCourse = isCourse; }
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

