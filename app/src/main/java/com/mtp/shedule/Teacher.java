package com.mtp.shedule;

public class Teacher {
    private String name;
    private String position;
    private String phone;
    private String email;
    private int colorResId; // lưu ID màu để set nền

    public Teacher(String name, String position, String phone, String email, int colorResId) {
        this.name = name;
        this.position = position;
        this.phone = phone;
        this.email = email;
        this.colorResId = colorResId;
    }

    public String getName() { return name; }
    public String getPosition() { return position; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public int getColorResId() { return colorResId; }
}
