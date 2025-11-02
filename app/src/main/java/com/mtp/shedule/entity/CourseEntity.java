package com.mtp.shedule.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "course")
public class CourseEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String title;
    private String teacher;
    private String room;
    private String timeStart;
    private String timeEnd;
    private String dayOfWeek;

    public CourseEntity(String title, String teacher, String room, String timeStart, String timeEnd, String dayOfWeek) {
        this.title = title;
        this.teacher = teacher;
        this.room = room;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this.dayOfWeek = dayOfWeek;
    }

    // Getters và Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTeacher() { return teacher; }
    public void setTeacher(String teacher) { this.teacher = teacher; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public String getTimeStart() { return timeStart; }
    public void setTimeStart(String timeStart) { this.timeStart = timeStart; }

    public String getTimeEnd() { return timeEnd; }
    public void setTimeEnd(String timeEnd) { this.timeEnd = timeEnd; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
}
