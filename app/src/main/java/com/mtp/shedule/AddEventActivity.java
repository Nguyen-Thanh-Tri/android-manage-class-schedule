package com.mtp.shedule;

import static com.mtp.shedule.SelectColorDialog.COLOR_MAPPING_DRAWABLE;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.EventEntity;
import com.mtp.shedule.notification.NotificationScheduler;

import java.util.Calendar;
import java.util.Locale;

public class AddEventActivity extends AppCompatActivity {

    EditText etTitle, etDescription, etTeacher, etRoom;
    Button btnStartDate, btnStartTime, btnEndDate, btnEndTime, btnColorPicker, btnSave, btnCancel;
    Spinner spinnerRepeatType, spinnerDayOfWeek, spinnerRemind;
    private final String[] REMINDER_DISPLAY = {
            "When event occurs", "5 minutes before", "15 minutes before",
            "30 minutes before", "1 hour before",
            "2 hours before", "1 day before",
            "2 day before", "7day before"
    };
    private final int[] REMINDER_VALUES_MINUTES = {
            0, 5, 15, 30, 60, 120, 1440, 2880, 10080
    };
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

        endCal.add(Calendar.HOUR_OF_DAY, 1);

        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etTeacher = findViewById(R.id.etTeacher);
        etRoom = findViewById(R.id.etRoom);
        spinnerRepeatType = findViewById(R.id.spinnerRepeatType);
        spinnerDayOfWeek = findViewById(R.id.spinnerDayOfWeek);
        spinnerRemind = findViewById(R.id.spinnerRemind);
        layoutCourseFields = findViewById(R.id.layoutCourseFields);
        btnStartDate = findViewById(R.id.btnStartDate);
        btnStartTime = findViewById(R.id.btnStartTime);
        btnEndDate = findViewById(R.id.btnEndDate);
        btnEndTime = findViewById(R.id.btnEndTime);
        btnColorPicker = findViewById(R.id.btnColorPicker);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        // GỌI YÊU CẦU QUYỀN THÔNG BÁO
        RequestPermission.requestNotificationPermission(this);

        setupSpinners();
        etTitle.addTextChangedListener(watcher);
        etDescription.addTextChangedListener(watcher);
        etTeacher.addTextChangedListener(watcher);
        etRoom.addTextChangedListener(watcher);

        //DateTime Pickers
        btnStartDate.setOnClickListener(v -> {
            pickStartDate();
            updateSaveButtonState();
        });
        btnStartTime.setOnClickListener(v -> {
            pickStartTime();
            updateSaveButtonState();
        });

        btnEndDate.setOnClickListener(v -> {
            pickEndDate();
            updateSaveButtonState();
        });

        btnEndTime.setOnClickListener(v -> {
            pickEndTime();
            updateSaveButtonState();
        });


        updateDateTimeButtons(startCal, btnStartDate, btnStartTime);
        updateDateTimeButtons(endCal, btnEndDate, btnEndTime);

        updateSaveButtonState();

        btnColorPicker.setBackgroundResource(COLOR_MAPPING_DRAWABLE[selectedColorIndex]);
        btnColorPicker.setOnClickListener(v ->chooseColor());

        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveEvent());


    }

    @SuppressLint("SetTextI18n")
    private void updateDateTimeButtons(Calendar cal, Button btnDate, Button btnTime) {
        // Cập nhật Button Ngày
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);
        btnDate.setText(day + "/" + (month + 1) + "/" + year);

        // Cập nhật Button Giờ
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        btnTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
    }

    // Pick start
    private void pickStartDate() {
        new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    startCal.set(year, month, day);
                    updateDateTimeButtons(startCal, btnStartDate, btnStartTime);

                    //Tự động cập nhật EndCal sau +1h
                    autoUpdateEndTime(true);
                    if (spinnerRepeatType.getSelectedItemPosition() == 1) {
                        int dayIndex = getDayOfWeekIndex(startCal);
                        spinnerDayOfWeek.setSelection(dayIndex);
                    }
                    updateSaveButtonState();
                },
                startCal.get(Calendar.YEAR),
                startCal.get(Calendar.MONTH),
                startCal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void pickStartTime() {
        new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    startCal.set(Calendar.HOUR_OF_DAY, hour);
                    startCal.set(Calendar.MINUTE, minute);
                    updateDateTimeButtons(startCal, btnStartDate, btnStartTime);

                    // Tự động cập nhật EndCal sau +1h
                    autoUpdateEndTime(true);
                    updateSaveButtonState();
                },
                startCal.get(Calendar.HOUR_OF_DAY),
                startCal.get(Calendar.MINUTE),
                true
        ).show();
    }

    //picker end
    private void pickEndDate() {
        new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    endCal.set(year, month, day);

                    // Cập nhật Button
                    updateDateTimeButtons(endCal, btnEndDate, btnEndTime);

                    validateAndAdjustEndTime();
                    updateSaveButtonState();
                },
                endCal.get(Calendar.YEAR),
                endCal.get(Calendar.MONTH),
                endCal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void pickEndTime() {
        new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    endCal.set(Calendar.HOUR_OF_DAY, hour);
                    endCal.set(Calendar.MINUTE, minute);

                    // Cập nhật Button
                    updateDateTimeButtons(endCal, btnEndDate, btnEndTime);

                    validateAndAdjustEndTime();
                    if (spinnerRepeatType.getSelectedItemPosition() == 1) {
                        int dayIndex = getDayOfWeekIndex(startCal);
                        spinnerDayOfWeek.setSelection(dayIndex);
                    }
                    updateSaveButtonState();
                },
                endCal.get(Calendar.HOUR_OF_DAY),
                endCal.get(Calendar.MINUTE),
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
                    int dayIndex = getDayOfWeekIndex(startCal);
                    spinnerDayOfWeek.setSelection(dayIndex);
                } else {
                    layoutCourseFields.setVisibility(android.view.View.GONE);
                }
                updateSaveButtonState();
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spinnerDayOfWeek.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                // Đảm bảo chỉ thực hiện khi chế độ là Weekly (Course)
                if (spinnerRepeatType.getSelectedItemPosition() == 1) {
                    int targetCalendarDay = getCalendarDayOfWeek(position);
                    updateStartAndEndDate(targetCalendarDay);
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        ArrayAdapter<String> remindAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, REMINDER_DISPLAY);
        remindAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRemind.setAdapter(remindAdapter);
        // Mặc định chọn "occurs" (Index 0)
        spinnerRemind.setSelection(0);
    }

    //Save Event
    private void saveEvent() {
        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        long start = startCal.getTimeInMillis();
        long end = endCal.getTimeInMillis();

        boolean isWeeklyRepeat = spinnerRepeatType.getSelectedItemPosition() == 1;

        if (title.isEmpty() ) {
            Toast.makeText(this, "Use your finger and punch on me, please", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endCal.before(startCal)) {
            Toast.makeText(this, "timeend sooner timestart? are you the Alien.", Toast.LENGTH_SHORT).show();
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
        int selectedReminderIndex = spinnerRemind.getSelectedItemPosition();
        int reminderMinutes = REMINDER_VALUES_MINUTES[selectedReminderIndex];
        event.setReminder(reminderMinutes); // SỬ DỤNG SETTER MỚI

        new Thread(() -> {
            long newId = db.eventDao().insertEvent(event);
            event.setId((int)newId);

            NotificationScheduler.scheduleReminder(this, event);
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

    // Tự động cập nhật endCal = startCal + 1h
    private void autoUpdateEndTime(boolean showAlert) {
        endCal.setTimeInMillis(startCal.getTimeInMillis());
        endCal.add(Calendar.HOUR_OF_DAY, 1);

        updateDateTimeButtons(endCal, btnEndDate, btnEndTime);

    }


    // Kiểm tra endCal > startCal
    private void validateAndAdjustEndTime() {
        if (endCal.before(startCal)) {
            // Thông báo lỗi
            Toast.makeText(this, "timeend sooner timestart? are you the Alien", Toast.LENGTH_LONG).show();

            // Tự động chỉnh endCal = startCal + 1h
            autoUpdateEndTime(false);
        }
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
    private int getDayOfWeekIndex(Calendar cal) {
        int androidDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

        // Nếu là Chủ nhật (1), thì index spinner là 6.
        if (androidDayOfWeek == Calendar.SUNDAY) {
            return 6;
        }
        // Nếu là Thứ Hai (2), index là 0. Thứ Bảy (7), index là 5.
        // Index = androidDayOfWeek - 2
        else {
            return androidDayOfWeek - 2;
        }
    }
    private int getCalendarDayOfWeek(int spinnerIndex) {
        // Spinner Index: 0=Monday, 6=Sunday
        // Calendar Day: 1=Sunday, 2=Monday, ..., 7=Saturday
        if (spinnerIndex == 6) {
            return Calendar.SUNDAY;
        } else {
            return spinnerIndex + 2; // Monday (0) -> 2, Tuesday (1) -> 3, etc.
        }
    }
    private void updateStartAndEndDate(int targetCalendarDay) {
        long duration = endCal.getTimeInMillis() - startCal.getTimeInMillis();

        Calendar tempCal = (Calendar) startCal.clone();
        int currentDay = tempCal.get(Calendar.DAY_OF_WEEK); // Ngày trong tuần hiện tại của startCal
        int daysToAdd = targetCalendarDay - currentDay;

        if (daysToAdd < 0) {
            daysToAdd += 7;
        }else if (daysToAdd == 0) {
        }

        tempCal.add(Calendar.DAY_OF_YEAR, daysToAdd);

        startCal.setTimeInMillis(tempCal.getTimeInMillis());

        endCal.setTimeInMillis(startCal.getTimeInMillis() + duration);
        updateDateTimeButtons(startCal, btnStartDate, btnStartTime);
        updateDateTimeButtons(endCal, btnEndDate, btnEndTime);
        updateSaveButtonState();
    }
    // Xử lý kết quả yêu cầu quyền từ hệ thống
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RequestPermission.PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Đã cấp quyền thông báo.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Không có quyền thông báo, có thể không nhận được nhắc nhở.", Toast.LENGTH_LONG).show();
                // Bạn có thể chọn vô hiệu hóa nút Save nếu không có quyền
            }
        }
    }
}