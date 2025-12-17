package com.mtp.shedule;

import static com.mtp.shedule.SelectColorDialog.COLOR_MAPPING_DRAWABLE;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.internal.TextWatcherAdapter;
import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.EventEntity;
import com.mtp.shedule.notification.NotificationScheduler; // Nếu bạn dùng WorkManager thì đổi import tương ứng

import java.util.Calendar;
import java.util.Locale;

public class AddEventActivity extends AppCompatActivity {

    // --- KHAI BÁO BIẾN UI ---
    EditText etTitle, etDescription, etTeacher, etRoom;
    Button btnStartDate, btnStartTime, btnEndDate, btnEndTime, btnColorPicker, btnSave, btnCancel;
    Spinner spinnerRepeatType, spinnerDayOfWeek, spinnerRemind;
    LinearLayout layoutCourseFields;

    // --- DỮ LIỆU CẤU HÌNH ---
    private final String[] REMINDER_DISPLAY = {
            "When event occurs", "5 minutes before", "15 minutes before",
            "30 minutes before", "1 hour before",
            "2 hours before", "1 day before",
            "2 day before", "7day before"
    };
    private final int[] REMINDER_VALUES_MINUTES = {
            0, 5, 15, 30, 60, 120, 1440, 2880, 10080
    };

    // --- BIẾN LOGIC ---
    private ConnDatabase db;
    // Calendar objects for picking date & time
    Calendar startCal = Calendar.getInstance();
    Calendar endCal = Calendar.getInstance();
    private int selectedColorIndex = 0; // Mặc định là Index 0 (Red)

    int selectedColor = Color.BLUE; // default
    // --- BIẾN TRẠNG THÁI CHẾ ĐỘ ---
    private boolean isViewMode = false;   // True = Xem/Sửa, False = Thêm mới
    private boolean isEditState = false;  // True = Đang nhập liệu để sửa
    private EventEntity currentEvent;     // Sự kiện nhận được từ Intent

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        db = ConnDatabase.getInstance(this);

        // 1. ÁNH XẠ VIEW
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

        // 2. YÊU CẦU QUYỀN VÀ SETUP SPINNER
        RequestPermission.requestNotificationPermission(this);
        setupSpinners();

        // 3. XỬ LÝ LOGIC CHẾ ĐỘ (VIEW vs ADD)
        Intent intent = getIntent();
        isViewMode = intent.getBooleanExtra("is_view_mode", false);

        if (isViewMode) {
            // --- CHẾ ĐỘ XEM / SỬA ---
            currentEvent = (EventEntity) intent.getSerializableExtra("event_item");
            if (currentEvent != null) {
                populateData();    // Đổ dữ liệu vào ô
                setupViewModeUI(); // Cài đặt giao diện Xem (Delete/Edit)
            }
        } else {
            // --- CHẾ ĐỘ THÊM MỚI (Mặc định) ---
            endCal.add(Calendar.HOUR_OF_DAY, 1);
            updateDateTimeButtons(startCal, btnStartDate, btnStartTime);
            updateDateTimeButtons(endCal, btnEndDate, btnEndTime);

            setupAddModeUI(); // Cài đặt giao diện Thêm (Cancel/Save)
        }

        // 4. CÀI ĐẶT LISTENER
        setupListeners();
    }

    // ==========================================
    // KHU VỰC SETUP GIAO DIỆN THEO CHẾ ĐỘ
    // ==========================================

    private void setupAddModeUI() {
        btnCancel.setText("Cancel");
        btnCancel.setTextColor(Color.BLACK);
        btnSave.setText("Save");

        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveNewEvent());

        enableInputFields(true); // Cho phép nhập liệu
    }

    private void setupViewModeUI() {
        // Nút Trái: DELETE (Màu đỏ)
        btnCancel.setText("Delete");
        btnCancel.setTextColor(Color.RED);
        btnCancel.setOnClickListener(v -> deleteEvent());

        // Nút Phải: EDIT
        btnSave.setText("Edit");
        btnSave.setOnClickListener(v -> toggleEditState());

        // Khóa không cho sửa
        enableInputFields(false);
    }

    private void toggleEditState() {
        if (!isEditState) {
            // -> Chuyển sang chế độ ĐANG SỬA
            isEditState = true;
            enableInputFields(true);
            btnSave.setText("Update");

            // Nút Cancel đổi chức năng thành "Hủy sửa"
            btnCancel.setText("Cancel");
            btnCancel.setTextColor(Color.BLACK);
            btnCancel.setOnClickListener(v -> {
                // Hủy sửa -> Quay lại chế độ xem
                isEditState = false;
                enableInputFields(false);
                btnSave.setText("Edit");
                populateData(); // Reset dữ liệu về ban đầu
                setupViewModeUI(); // Reset nút Delete
            });

        } else {
            // -> Đang sửa -> Bấm Update -> Lưu
            updateExistingEvent();
        }
    }

    // ==========================================
    // KHU VỰC XỬ LÝ DỮ LIỆU & DATABASE
    // ==========================================

    // 1. ĐỔ DỮ LIỆU TỪ OBJECT VÀO GIAO DIỆN
    private void populateData() {
        etTitle.setText(currentEvent.getTitle());
        etDescription.setText(currentEvent.getDescription());

        // Thời gian
        startCal.setTimeInMillis(currentEvent.getStartTime());
        endCal.setTimeInMillis(currentEvent.getEndTime());
        updateDateTimeButtons(startCal, btnStartDate, btnStartTime);
        updateDateTimeButtons(endCal, btnEndDate, btnEndTime);

        // Màu
        selectedColorIndex = currentEvent.getColor();
        if(selectedColorIndex >= 0 && selectedColorIndex < COLOR_MAPPING_DRAWABLE.length) {
            btnColorPicker.setBackgroundResource(COLOR_MAPPING_DRAWABLE[selectedColorIndex]);
        }

        // Nhắc nhở (Tìm index tương ứng với số phút)
        int minutes = currentEvent.getReminder();
        for (int i = 0; i < REMINDER_VALUES_MINUTES.length; i++) {
            if (REMINDER_VALUES_MINUTES[i] == minutes) {
                spinnerRemind.setSelection(i);
                break;
            }
        }

        // Lặp lại / Course
        if (currentEvent.isCourse() || "weekly".equals(currentEvent.getRepeatType())) {
            spinnerRepeatType.setSelection(1); // Weekly
            etTeacher.setText(currentEvent.getTeacher());
            etRoom.setText(currentEvent.getRoom());
            layoutCourseFields.setVisibility(View.VISIBLE);

            // Xử lý dayOfWeek cho spinner nếu cần (đơn giản hóa ở đây)
        } else {
            spinnerRepeatType.setSelection(0); // None
            layoutCourseFields.setVisibility(View.GONE);
        }
    }

    // 2. LẤY DỮ LIỆU TỪ GIAO DIỆN RA OBJECT
    private EventEntity buildEventFromInput() {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(this, "Please enter title", Toast.LENGTH_SHORT).show();
            return null;
        }

        String desc = etDescription.getText().toString().trim();
        long start = startCal.getTimeInMillis();
        long end = endCal.getTimeInMillis();

        if (end < start) {
            Toast.makeText(this, "End time cannot be before start time", Toast.LENGTH_SHORT).show();
            return null;
        }

        EventEntity event = new EventEntity();
        event.setTitle(title);
        event.setDescription(desc);
        event.setStartTime(start);
        event.setEndTime(end);
        event.setColor(selectedColorIndex);

        int selectedReminderIndex = spinnerRemind.getSelectedItemPosition();
        event.setReminder(REMINDER_VALUES_MINUTES[selectedReminderIndex]);

        if (spinnerRepeatType.getSelectedItemPosition() == 1) { // Weekly
            event.setRepeatType("weekly");
            event.setIsCourse(true);
            event.setTeacher(etTeacher.getText().toString().trim());
            event.setRoom(etRoom.getText().toString().trim());

            // Lấy thứ trong tuần từ Spinner
            String[] days = {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
            event.setDayOfWeek(days[spinnerDayOfWeek.getSelectedItemPosition()]);
        } else {
            event.setRepeatType("none");
            event.setIsCourse(false);
        }
        return event;
    }

    // 3. LƯU MỚI (INSERT)
    private void saveNewEvent() {
        EventEntity event = buildEventFromInput();
        if (event == null) return;

        // Nếu là Weekly, tính lại ngày để đảm bảo vào đúng thứ
        if ("weekly".equals(event.getRepeatType())) {
            Calendar temp = Calendar.getInstance();
            temp.setTimeInMillis(event.getStartTime());

            Calendar correctedStart = getNextOccurringDayOfWeek(
                    event.getDayOfWeek(),
                    temp.get(Calendar.HOUR_OF_DAY),
                    temp.get(Calendar.MINUTE)
            );

            long duration = event.getEndTime() - event.getStartTime();
            event.setStartTime(correctedStart.getTimeInMillis());
            event.setEndTime(correctedStart.getTimeInMillis() + duration);
        }

        new Thread(() -> {
            long newId = db.eventDao().insertEvent(event);
            event.setId((int)newId);

            // Đặt lịch thông báo
            NotificationScheduler.scheduleReminder(this, event);

            runOnUiThread(() -> {
                Toast.makeText(this, "Event saved!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            });
        }).start();
    }

    // 4. CẬP NHẬT (UPDATE)
    private void updateExistingEvent() {
        EventEntity updatedEvent = buildEventFromInput();
        if (updatedEvent == null) return;

        // Giữ ID cũ (lấy giá trị tuyệt đối để tránh lỗi với ID âm)
        int originalId = Math.abs(currentEvent.getId());
        updatedEvent.setId(originalId);

        new Thread(() -> {
            db.eventDao().updateEvent(updatedEvent);

            // Cập nhật lại thông báo
            NotificationScheduler.scheduleReminder(this, updatedEvent);

            runOnUiThread(() -> {
                Toast.makeText(this, "Updated successfully!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            });
        }).start();
    }

    // 5. XÓA (DELETE)
    private void deleteEvent() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Event")
                .setMessage("Are you sure you want to delete this event?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    new Thread(() -> {
                        int idToDelete = Math.abs(currentEvent.getId());
                        EventEntity eventToDelete = db.eventDao().getEventById(idToDelete);

                        if (eventToDelete != null) {
                            db.eventDao().deleteEvent(eventToDelete);
                            // Hủy thông báo
                            NotificationScheduler.cancelReminder(this, idToDelete);
                        }

                        runOnUiThread(() -> {
                            Toast.makeText(this, "Deleted!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ==========================================
    // CÁC HÀM HỖ TRỢ UI KHÁC
    // ==========================================

    private void enableInputFields(boolean enable) {
        etTitle.setEnabled(enable);
        etDescription.setEnabled(enable);
        etTeacher.setEnabled(enable);
        etRoom.setEnabled(enable);
        btnStartDate.setEnabled(enable);
        btnStartTime.setEnabled(enable);
        btnEndDate.setEnabled(enable);
        btnEndTime.setEnabled(enable);
        btnColorPicker.setEnabled(enable);
        spinnerRepeatType.setEnabled(enable);
        spinnerDayOfWeek.setEnabled(enable);
        spinnerRemind.setEnabled(enable);
    }

    private void setupListeners() {
        etTitle.addTextChangedListener(watcher);
        etDescription.addTextChangedListener(watcher);
        etTeacher.addTextChangedListener(watcher);
        etRoom.addTextChangedListener(watcher);

        btnStartDate.setOnClickListener(v -> { pickStartDate(); updateSaveButtonState(); });
        btnStartTime.setOnClickListener(v -> { pickStartTime(); updateSaveButtonState(); });
        btnEndDate.setOnClickListener(v -> { pickEndDate(); updateSaveButtonState(); });
        btnEndTime.setOnClickListener(v -> { pickEndTime(); updateSaveButtonState(); });

        btnColorPicker.setOnClickListener(v -> chooseColor());
    }

    private void chooseColor() {
        SelectColorDialog dialog = new SelectColorDialog();
        dialog.setSelectedColorIndex(selectedColorIndex);
        dialog.setOnColorSelectedListener(colorIndex -> {
            selectedColorIndex = colorIndex;
            btnColorPicker.setBackgroundResource(COLOR_MAPPING_DRAWABLE[selectedColorIndex]);
        });
        dialog.show(getSupportFragmentManager(), "ColorDialog");
    }

    private void pickStartDate() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            startCal.set(year, month, day);
            updateDateTimeButtons(startCal, btnStartDate, btnStartTime);
            autoUpdateEndTime(true);

            // Auto select day of week based on date
            if (spinnerRepeatType.getSelectedItemPosition() == 1) {
                int dayIndex = getDayOfWeekIndex(startCal);
                spinnerDayOfWeek.setSelection(dayIndex);
            }
            updateSaveButtonState();
        }, startCal.get(Calendar.YEAR), startCal.get(Calendar.MONTH), startCal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pickStartTime() {
        new TimePickerDialog(this, (view, hour, minute) -> {
            startCal.set(Calendar.HOUR_OF_DAY, hour);
            startCal.set(Calendar.MINUTE, minute);
            updateDateTimeButtons(startCal, btnStartDate, btnStartTime);
            autoUpdateEndTime(true);
            updateSaveButtonState();
        }, startCal.get(Calendar.HOUR_OF_DAY), startCal.get(Calendar.MINUTE), true).show();
    }

    private void pickEndDate() {
        new DatePickerDialog(this, (view, year, month, day) -> {
            endCal.set(year, month, day);
            updateDateTimeButtons(endCal, btnEndDate, btnEndTime);
            validateAndAdjustEndTime();
            updateSaveButtonState();
        }, endCal.get(Calendar.YEAR), endCal.get(Calendar.MONTH), endCal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void pickEndTime() {
        new TimePickerDialog(this, (view, hour, minute) -> {
            endCal.set(Calendar.HOUR_OF_DAY, hour);
            endCal.set(Calendar.MINUTE, minute);
            updateDateTimeButtons(endCal, btnEndDate, btnEndTime);
            validateAndAdjustEndTime();
            updateSaveButtonState();
        }, endCal.get(Calendar.HOUR_OF_DAY), endCal.get(Calendar.MINUTE), true).show();
    }

    @SuppressLint("SetTextI18n")
    private void updateDateTimeButtons(Calendar cal, Button btnDate, Button btnTime) {
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int month = cal.get(Calendar.MONTH);
        int year = cal.get(Calendar.YEAR);
        btnDate.setText(day + "/" + (month + 1) + "/" + year);

        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        btnTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
    }

    private void autoUpdateEndTime(boolean showAlert) {
        endCal.setTimeInMillis(startCal.getTimeInMillis());
        endCal.add(Calendar.HOUR_OF_DAY, 1);
        updateDateTimeButtons(endCal, btnEndDate, btnEndTime);
    }

    private void validateAndAdjustEndTime() {
        if (endCal.before(startCal)) {
            Toast.makeText(this, "End time adjusted to be after start time", Toast.LENGTH_SHORT).show();
            autoUpdateEndTime(false);
        }
    }

    // Setup spinners for repeat type and day of week
    private void setupSpinners() {
        // Repeat Type
        ArrayAdapter<String> repeatAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"None (One-time)", "Weekly (Course)"});
        repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRepeatType.setAdapter(repeatAdapter);

        // Day of Week
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"});
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDayOfWeek.setAdapter(dayAdapter);

        // Reminder
        ArrayAdapter<String> remindAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, REMINDER_DISPLAY);
        remindAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRemind.setAdapter(remindAdapter);
        spinnerRemind.setSelection(0);

        // Listener logic
        spinnerRepeatType.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position == 1) { // Weekly
                    layoutCourseFields.setVisibility(View.VISIBLE);
                    int dayIndex = getDayOfWeekIndex(startCal);
                    spinnerDayOfWeek.setSelection(dayIndex);
                } else {
                    layoutCourseFields.setVisibility(View.GONE);
                }
                updateSaveButtonState();
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        spinnerDayOfWeek.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (spinnerRepeatType.getSelectedItemPosition() == 1) {
                    int targetCalendarDay = getCalendarDayOfWeek(position);
                    updateStartAndEndDate(targetCalendarDay);
                }
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private int getDayOfWeekIndex(Calendar cal) {
        int androidDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        // Sunday (1) -> 6, Monday (2) -> 0, ... Saturday (7) -> 5
        if (androidDayOfWeek == Calendar.SUNDAY) return 6;
        else return androidDayOfWeek - 2;
    }

    private int getCalendarDayOfWeek(int spinnerIndex) {
        if (spinnerIndex == 6) return Calendar.SUNDAY;
        else return spinnerIndex + 2;
    }

    private void updateStartAndEndDate(int targetCalendarDay) {
        long duration = endCal.getTimeInMillis() - startCal.getTimeInMillis();
        Calendar tempCal = (Calendar) startCal.clone();
        int currentDay = tempCal.get(Calendar.DAY_OF_WEEK);
        int daysToAdd = targetCalendarDay - currentDay;

        if (daysToAdd < 0) daysToAdd += 7;

        // Chỉ cập nhật nếu có sự thay đổi thực sự để tránh loop
        if (daysToAdd != 0) {
            tempCal.add(Calendar.DAY_OF_YEAR, daysToAdd);
            startCal.setTimeInMillis(tempCal.getTimeInMillis());
            endCal.setTimeInMillis(startCal.getTimeInMillis() + duration);
            updateDateTimeButtons(startCal, btnStartDate, btnStartTime);
            updateDateTimeButtons(endCal, btnEndDate, btnEndTime);
        }
    }

    TextWatcher watcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
        @Override public void afterTextChanged(Editable s) { updateSaveButtonState(); }
    };

    private void updateSaveButtonState() {
        if (isViewMode && !isEditState) {
            // Đang ở chế độ xem thì không cần check nút save (vì nó là nút Edit)
            btnSave.setEnabled(true);
            return;
        }

        boolean enable = !etTitle.getText().toString().trim().isEmpty();
        if (spinnerRepeatType.getSelectedItemPosition() == 1) {
            enable = enable && !etTeacher.getText().toString().trim().isEmpty()
                    && !etRoom.getText().toString().trim().isEmpty();
        }
        btnSave.setEnabled(enable);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RequestPermission.PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Notification permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notification permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

    private Calendar getNextOccurringDayOfWeek(String dayName, int hour, int minute) {
        int targetDay = parseDayOfWeekString(dayName);
        Calendar cal = Calendar.getInstance(Locale.getDefault());

        // Lưu thời điểm "ngay bây giờ" để so sánh
        long now = System.currentTimeMillis();

        // Thiết lập cho ngày hôm nay với giờ/phút đã chọn
        cal.set(Calendar.HOUR_OF_DAY, hour); // 0h sẽ là 0
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        // LOGIC QUAN TRỌNG:
        // Nếu hôm nay đúng là Thứ đó NHƯNG giờ đã trôi qua (ví dụ đặt 0h10 lúc đang là 0h15)
        // HOẶC hôm nay không phải Thứ đó.
        if (cal.getTimeInMillis() <= now || cal.get(Calendar.DAY_OF_WEEK) != targetDay) {
            // Tìm ngày tiếp theo khớp với Thứ đó
            do {
                cal.add(Calendar.DAY_OF_YEAR, 1);
            } while (cal.get(Calendar.DAY_OF_WEEK) != targetDay);
        }

        return cal;
    }
    private int parseDayOfWeekString(String day) {
        switch (day.toLowerCase().trim()) {
            case "sunday": return Calendar.SUNDAY;
            case "tuesday": return Calendar.TUESDAY;
            case "wednesday": return Calendar.WEDNESDAY;
            case "thursday": return Calendar.THURSDAY;
            case "friday": return Calendar.FRIDAY;
            case "saturday": return Calendar.SATURDAY;
            default: return Calendar.MONDAY;
        }
    }
}