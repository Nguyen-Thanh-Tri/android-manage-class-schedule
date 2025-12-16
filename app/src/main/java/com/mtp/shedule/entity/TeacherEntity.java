package com.mtp.shedule.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.mtp.shedule.R;

@Entity(tableName = "teacher")
public class TeacherEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String name;
    private String position;
    private String phone;
    private String email;
    private int color;

    public TeacherEntity(String name, String position, String phone, String email) {
        this.name = name;
        this.position = position;
        this.phone = phone;
        this.email = email;
        this.color = R.drawable.gradient_bg_red; // mặc định
    }

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
