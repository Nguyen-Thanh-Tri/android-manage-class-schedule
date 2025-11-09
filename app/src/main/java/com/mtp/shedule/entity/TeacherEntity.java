package com.mtp.shedule.entity;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "teacher")
public class TeacherEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private String position;
    private String phone;
    private String email;
    private int colorResId; // lưu ID màu để set nền

    public TeacherEntity(String name, String position, String phone, String email, int colorResId) {
        this.name = name;
        this.position = position;
        this.phone = phone;
        this.email = email;
        this.colorResId = colorResId;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public String getPosition() { return position; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public int getColorResId() { return colorResId; }
}
