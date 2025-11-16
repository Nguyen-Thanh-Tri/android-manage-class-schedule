package com.mtp.shedule;

import static com.mtp.shedule.SelectColorDialog.COLOR_MAPPING_DRAWABLE;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.media.metrics.Event;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.internal.TextWatcherAdapter;
import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.EventEntity;

import java.util.Calendar;

public class AddEventActivity extends AppCompatActivity {

    EditText etTitle, etDescription, etTeacher, etRoom;
    Button btnStartDate, btnStartTime, btnEndDate, btnEndTime, btnColorPicker, btnSave, btnCancel;
    Spinner spinnerRepeatType, spinnerDayOfWeek;
    LinearLayout layoutCourseFields;
    private ConnDatabase db;
    // Calendar objects for picking date & time
    Calendar startCal = Calendar.getInstance();
    Calendar endCal = Calendar.getInstance();
    private int selectedColorIndex = 0; // Mặc định là Index 0 (Red)

    int selectedColor = Color.BLUE; // default

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        db = ConnDatabase.getInstance(this);

        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etTeacher = findViewById(R.id.etTeacher);
        etRoom = findViewById(R.id.etRoom);
        spinnerRepeatType = findViewById(R.id.spinnerRepeatType);
        spinnerDayOfWeek = findViewById(R.id.spinnerDayOfWeek);
        layoutCourseFields = findViewById(R.id.layoutCourseFields);
        btnStartDate = findViewById(R.id.btnStartDate);
        btnStartTime = findViewById(R.id.btnStartTime);
        btnEndDate = findViewById(R.id.btnEndDate);
        btnEndTime = findViewById(R.id.btnEndTime);
        btnColorPicker = findViewById(R.id.btnColorPicker);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        setupSpinners();
        etTitle.addTextChangedListener(watcher);
        etDescription.addTextChangedListener(watcher);
        etTeacher.addTextChangedListener(watcher);
        etRoom.addTextChangedListener(watcher);

        //DateTime Pickers
        btnStartDate.setOnClickListener(v -> {
            pickDate(startCal, btnStartDate);
            updateSaveButtonState();
        });
        btnStartTime.setOnClickListener(v -> {
            pickTime(startCal, btnStartTime);
            updateSaveButtonState();
        });

        btnEndDate.setOnClickListener(v -> {
            pickDate(endCal, btnEndDate);
            updateSaveButtonState();
        });

        btnEndTime.setOnClickListener(v -> {
            pickTime(endCal, btnEndTime);
            updateSaveButtonState();
        });
        updateSaveButtonState();

        btnColorPicker.setBackgroundResource(COLOR_MAPPING_DRAWABLE[selectedColorIndex]);
        btnColorPicker.setOnClickListener(v ->chooseColor());

        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveEvent());


    }

    // Pick Date
    @SuppressLint("SetTextI18n")
    private void pickDate(Calendar cal, Button btn) {
        new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DAY_OF_MONTH, day);
                    btn.setText(day + "/" + (month + 1) + "/" + year);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    // Pick Time
    @SuppressLint("DefaultLocale")
    private void pickTime(Calendar cal, Button btn) {
        new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    cal.set(Calendar.HOUR_OF_DAY, hour);
                    cal.set(Calendar.MINUTE, minute);
                    btn.setText(String.format("%02d:%02d", hour, minute));
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
        ).show();
    }

    //Pick Color
    private void chooseColor() {
        SelectColorDialog dialog = new SelectColorDialog();

        // Truyền mã màu INT để SelectColorDialog đánh dấu ô màu đúng
        dialog.setSelectedColorIndex(selectedColorIndex);

        dialog.setOnColorSelectedListener(colorIndex -> {

            selectedColorIndex = colorIndex;
            btnColorPicker.setBackgroundResource(COLOR_MAPPING_DRAWABLE[selectedColorIndex]);
        });
        dialog.show(getSupportFragmentManager(), "ColorDialog");
    }

    // Setup spinners for repeat type and day of week
    private void setupSpinners() {
        // Setup repeat type spinner
        String[] repeatTypes = {"None (One-time)", "Weekly (Course)"};
        ArrayAdapter<String> repeatAdapter = new ArrayAdapter<>(this, 
            android.R.layout.simple_spinner_item, repeatTypes);
        repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRepeatType.setAdapter(repeatAdapter);
        
        // Setup day of week spinner  
        String[] daysOfWeek = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(this,
            android.R.layout.simple_spinner_item, daysOfWeek);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDayOfWeek.setAdapter(dayAdapter);
        
        // Show/hide course fields based on repeat type selection
        spinnerRepeatType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position == 1) { // Weekly (Course) selected
                    layoutCourseFields.setVisibility(android.view.View.VISIBLE);
                } else {
                    layoutCourseFields.setVisibility(android.view.View.GONE);
                }
                updateSaveButtonState();
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    //Save Event
    private void saveEvent() {
        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        long start = startCal.getTimeInMillis();
        long end = endCal.getTimeInMillis();
        
        boolean isWeeklyRepeat = spinnerRepeatType.getSelectedItemPosition() == 1;
        
        // Basic validation
        if (title.isEmpty() || desc.isEmpty() || start == 0 || end == 0) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }
        
        EventEntity event;
        
        if (isWeeklyRepeat) {
            // Create course event
            String teacher = etTeacher.getText().toString().trim();
            String room = etRoom.getText().toString().trim();
            
            if (teacher.isEmpty() || room.isEmpty()) {
                Toast.makeText(this, "Please fill teacher and room for course events", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Get selected day of week
            String[] days = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
            String selectedDay = days[spinnerDayOfWeek.getSelectedItemPosition()];
            
            // Create course event using course constructor
            event = new EventEntity(title, teacher, room, selectedDay, start, end);
        } else {
            // Create regular one-time event
            event = new EventEntity(title, desc, start, end);
        }
        
        event.setColor(selectedColorIndex);

        new Thread(() -> {
            db.eventDao().insertEvent(event);
            
            // Notify fragments about the new event on the UI thread
            runOnUiThread(() -> {
                // Send result to notify fragments about event creation
                Bundle result = new Bundle();
                result.putString("message", "Event created successfully");
                getSupportFragmentManager().setFragmentResult("event_created", result);
            });
        }).start();

        String eventType = isWeeklyRepeat ? "Course" : "Event";
        Toast.makeText(this, eventType + " saved!", Toast.LENGTH_SHORT).show();
        
        // Set result for calling activity/fragment
        setResult(RESULT_OK);
        finish();
    }

    TextWatcher watcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) {
            updateSaveButtonState();
        }
    };

    private void updateSaveButtonState() {
        boolean isTitleFilled = !etTitle.getText().toString().trim().isEmpty();
        boolean isDescFilled = !etDescription.getText().toString().trim().isEmpty();
        boolean isStartSet = startCal.get(Calendar.YEAR) > 1970;
        boolean isEndSet = endCal.get(Calendar.YEAR) > 1970;
        
        boolean enable = isTitleFilled && isDescFilled && isStartSet && isEndSet;
        
        // Additional validation for course events
        if (spinnerRepeatType.getSelectedItemPosition() == 1) { // Weekly (Course)
            boolean isTeacherFilled = !etTeacher.getText().toString().trim().isEmpty();
            boolean isRoomFilled = !etRoom.getText().toString().trim().isEmpty();
            enable = enable && isTeacherFilled && isRoomFilled;
        }
        
        btnSave.setEnabled(enable);
    }
}