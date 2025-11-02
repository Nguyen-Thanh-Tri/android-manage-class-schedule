package com.mtp.shedule;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mtp.shedule.database.CourseDatabase;
import com.mtp.shedule.entity.ExamEntity;
import com.mtp.shedule.dao.ExamDao;

public class AddExamActivity extends AppCompatActivity {
    private void showDatePicker(EditText editText) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        int year = c.get(java.util.Calendar.YEAR);
        int month = c.get(java.util.Calendar.MONTH);
        int day = c.get(java.util.Calendar.DAY_OF_MONTH);

        android.app.DatePickerDialog datePicker = new android.app.DatePickerDialog(this,
                (view, year1, month1, dayOfMonth) -> editText.setText(String.format("%04d-%02d-%02d", year1, month1 + 1, dayOfMonth)),
                year, month, day);
        datePicker.show();
    }

    private void showTimePicker(EditText editText) {
        java.util.Calendar c = java.util.Calendar.getInstance();
        int hour = c.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = c.get(java.util.Calendar.MINUTE);

        android.app.TimePickerDialog timePicker = new android.app.TimePickerDialog(this,
                (view, hourOfDay, minute1) -> editText.setText(String.format("%02d:%02d", hourOfDay, minute1)),
                hour, minute, true);
        timePicker.show();
    }
    private EditText etSubject, etDate, etTime, etLocation;
    private Button btnSave;
    private CourseDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_exam);

        etSubject = findViewById(R.id.etExamSubject);
        etDate = findViewById(R.id.etExamDate);
        etTime = findViewById(R.id.etExamTime);
        etLocation = findViewById(R.id.etExamLocation);
        btnSave = findViewById(R.id.btnSaveExam);

        db = CourseDatabase.getInstance(this);

    // DatePicker for Exam Date
    etDate.setOnClickListener(v -> showDatePicker(etDate));
    // TimePicker for Exam Time
    etTime.setOnClickListener(v -> showTimePicker(etTime));

        btnSave.setOnClickListener(v -> {
            String subject = etSubject.getText().toString().trim();
            String date = etDate.getText().toString().trim();
            String time = etTime.getText().toString().trim();
            String location = etLocation.getText().toString().trim();

            if (subject.isEmpty() || date.isEmpty() || time.isEmpty() || location.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            ExamEntity exam = new ExamEntity(subject, date, time, location);
            new Thread(() -> {
                db.examDao().insert(exam);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Exam added successfully", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }).start();
        });
    }
}
