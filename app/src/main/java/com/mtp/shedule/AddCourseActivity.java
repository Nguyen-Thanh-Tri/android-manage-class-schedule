package com.mtp.shedule;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.Intent;

public class AddCourseActivity extends AppCompatActivity {

    EditText etTitle, etTeacher, etRoom, etStartTime, etEndTime;
    Button btnSave;
    DatabaseHelper db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_course);

        db = new DatabaseHelper(this);

        etTitle = findViewById(R.id.etTitle);
        etTeacher = findViewById(R.id.etTeacher);
        etRoom = findViewById(R.id.etRoom);
        etStartTime = findViewById(R.id.etStartTime);
        etEndTime = findViewById(R.id.etEndTime);
        btnSave = findViewById(R.id.btnSave);

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String teacher = etTeacher.getText().toString().trim();
            String room = etRoom.getText().toString().trim();
            String start = etStartTime.getText().toString().trim();
            String end = etEndTime.getText().toString().trim();

            if (title.isEmpty() || teacher.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Course course = new Course(0, title, teacher, room, start, end);
            db.addCourse(course);
            Toast.makeText(this, "Course added successfully", Toast.LENGTH_SHORT).show();

            // Quay lại trang chính
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}

