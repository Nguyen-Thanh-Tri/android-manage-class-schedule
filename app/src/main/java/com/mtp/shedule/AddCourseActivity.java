package com.mtp.shedule;

import androidx.appcompat.app.AppCompatActivity;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.content.Intent;

import java.util.Calendar;

public class AddCourseActivity extends AppCompatActivity {

    EditText etTitle, etTeacher, etRoom, etStartTime, etEndTime;
    Spinner spinnerDay;
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
        spinnerDay = findViewById(R.id.spinnerDay);
        btnSave = findViewById(R.id.btnSave);

        String[] days = {"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, days);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDay.setAdapter(adapter);

        // --- TimePicker cho Start Time ---
        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime));

        // --- TimePicker cho End Time ---
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime));

        btnSave.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String teacher = etTeacher.getText().toString().trim();
            String room = etRoom.getText().toString().trim();
            String start = etStartTime.getText().toString().trim();
            String end = etEndTime.getText().toString().trim();
            String day = spinnerDay.getSelectedItem().toString();

            if (title.isEmpty() || teacher.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Course course = new Course(0, title, teacher, room, start, end, day);
            db.addCourse(course);
            Toast.makeText(this, "Course added successfully", Toast.LENGTH_SHORT).show();

            // Quay lại trang chính
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private void showTimePicker(EditText editText){
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePicker = new TimePickerDialog(this,
                (view, hourOfDay, minute1) -> editText.setText(String.format("%02d:%02d", hourOfDay, minute1)),
                hour, minute, true);
        timePicker.show();
    }
}

