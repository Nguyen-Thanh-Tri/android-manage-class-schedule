package com.mtp.shedule.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.mtp.shedule.R;

@Entity(tableName = "exam")
public class ExamEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String subject;
    private String date;
    private String time;
    private String location;
    private int color;

    public ExamEntity(String subject, String date, String time, String location) {
        this.subject = subject;
        this.date = date;
        this.time = time;
        this.location = location;
        this.color = R.drawable.gradient_bg_red;
    }

    // Getters and setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
}
