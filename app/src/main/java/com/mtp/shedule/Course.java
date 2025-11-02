package com.mtp.shedule;

public class Course {
    private int id;
    private String title;
    private String teacher;
    private String room;
    private String timeStart;
    private String timeEnd;
    private String dayOfWeek;

    public Course(int id, String title, String teacher, String room, String timeStart, String timeEnd, String dayOfWeek) {
        this.id = id;
        this.title = title;
        this.teacher = teacher;
        this.room = room;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
        this .dayOfWeek = dayOfWeek;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getTeacher() { return teacher; }
    public String getRoom() { return room; }
    public String getTimeStart() { return timeStart; }
    public String getTimeEnd() { return timeEnd; }
    public String getDayOfWeek() {return dayOfWeek;}
}
