package com.mtp.shedule.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.io.Serializable;

@Entity(tableName = "teacher")
public class TeacherEntity implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String name;
    private String position;
    private String phone;
    private String email;
    private int color; // Lưu index (0, 1, 2...)

    public TeacherEntity(String name, String position, String phone, String email, int color) {
        this.name = name;
        this.position = position;
        this.phone = phone;
        this.email = email;
        this.color = color;
    }

    // Constructor rỗng cho Room
    public TeacherEntity() {}

    // Getters và Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public int getColor() { return color; }
    public void setColor(int color) { this.color = color; }
}