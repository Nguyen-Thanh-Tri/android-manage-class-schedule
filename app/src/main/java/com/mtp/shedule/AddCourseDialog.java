package com.mtp.shedule;

import static com.mtp.shedule.SelectColorDialog.COLOR_MAPPING_DRAWABLE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.EventEntity;
import com.mtp.shedule.notification.NotificationScheduler;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddCourseDialog extends DialogFragment {

    EditText etTitle, etTeacher, etRoom, etStartTime, etEndTime;
    Spinner spinnerDay;
    Button btnSave, btnCancel, btnSelectColor;
    private ConnDatabase db;

    private int selectedColorIndex = 0; // Mặc định là Index 0 (Red)
    private static final String ARG_EVENT_ITEM = "event_item";
    private int courseId = -1;
    private boolean isViewOnly = false;
    private boolean isEditMode = false;
    private EventEntity currentEvent;

    public static AddCourseDialog newInstance(EventEntity courseEvent) {
        AddCourseDialog dialog = new AddCourseDialog();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EVENT_ITEM, courseEvent);
        dialog.setArguments(args);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_add_course, container, false);

        etTitle = view.findViewById(R.id.etTitle);
        etTeacher = view.findViewById(R.id.etTeacher);
        etRoom = view.findViewById(R.id.etRoom);
        etStartTime = view.findViewById(R.id.etStartTime);
        etEndTime = view.findViewById(R.id.etEndTime);
        spinnerDay = view.findViewById(R.id.spinnerDay);
        btnSave = view.findViewById(R.id.btnSave);
        btnCancel = view.findViewById(R.id.btnCancel);
        btnSelectColor = view.findViewById(R.id.btnSelectColor);

        List<String> days = Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, days);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDay.setAdapter(adapter);

        db = ConnDatabase.getInstance(requireContext());

        // Đăng ký TextWatcher cho validation
        etTitle.addTextChangedListener(validationWatcher);
        etTeacher.addTextChangedListener(validationWatcher);
        etRoom.addTextChangedListener(validationWatcher);
        etStartTime.addTextChangedListener(validationWatcher);
        etEndTime.addTextChangedListener(validationWatcher);

        //update
        if (getArguments() != null && getArguments().containsKey(ARG_EVENT_ITEM)) {
            isEditMode = true;
            isViewOnly = true;
            currentEvent = (EventEntity) getArguments().getSerializable(ARG_EVENT_ITEM);
            if (currentEvent != null) {
                fillData(days);
            }
        }

        // Setup UI theo Mode
        setupModeUI();

        // Listeners
        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime, true));
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime, false));
        btnCancel.setOnClickListener(v -> dismiss());

        btnSelectColor.setOnClickListener(v ->{
            SelectColorDialog dialog = new SelectColorDialog();
            // Truyền mã màu INT để SelectColorDialog đánh dấu ô màu đúng
            dialog.setSelectedColorIndex(selectedColorIndex);
            dialog.setOnColorSelectedListener(colorIndex -> {
                selectedColorIndex = colorIndex;
                btnSelectColor.setBackgroundResource(COLOR_MAPPING_DRAWABLE[selectedColorIndex]);
            });
            dialog.show(getParentFragmentManager(), "ColorDialog");
        });
        return view;
    }

    private void fillData(List<String> days) {
        etTitle.setText(currentEvent.getTitle());
        etTeacher.setText(currentEvent.getTeacher());
        etRoom.setText(currentEvent.getRoom());
        etStartTime.setText(currentEvent.getStartTimeFormatted());
        etEndTime.setText(currentEvent.getEndTimeFormatted());
        selectedColorIndex = currentEvent.getColor();
        btnSelectColor.setBackgroundResource(COLOR_MAPPING_DRAWABLE[selectedColorIndex]);

        String day = currentEvent.getDayOfWeek();
        if (day != null && !day.isEmpty()) {
            String formattedDay = day.substring(0, 1).toUpperCase() + day.substring(1).toLowerCase();
            int pos = days.indexOf(formattedDay);
            if (pos >= 0) spinnerDay.setSelection(pos);
        }
    }

    private void setupModeUI() {
        boolean canEdit = !isViewOnly;
        etTitle.setEnabled(canEdit);
        etTeacher.setEnabled(canEdit);
        etRoom.setEnabled(canEdit);
        etStartTime.setEnabled(canEdit);
        etEndTime.setEnabled(canEdit);
        spinnerDay.setEnabled(canEdit);
        btnSelectColor.setEnabled(canEdit);

        if (isViewOnly) {
            btnSave.setText("Update");
            btnSave.setEnabled(true);
            btnSave.setAlpha(1.0f);
            btnSave.setOnClickListener(v -> {
                isViewOnly = false;
                setupModeUI();
            });
        } else {
            btnSave.setText(isEditMode ? "Confirm Update" : "Save");
            btnSave.setOnClickListener(v -> saveCourseAsEvent());
            validateFields();
        }
    }

    private void saveCourseAsEvent() {
        String title = etTitle.getText().toString().trim();
        String teacher = etTeacher.getText().toString().trim();
        String room = etRoom.getText().toString().trim();
        String startStr = etStartTime.getText().toString().trim();
        String endStr = etEndTime.getText().toString().trim();
        String dayOfWeek = spinnerDay.getSelectedItem().toString();

        String[] sParts = startStr.split(":");
        int startH = Integer.parseInt(sParts[0]);
        int startM = Integer.parseInt(sParts[1]);

        Calendar startCal = getNextOccurringDayOfWeek(dayOfWeek, startH, startM);

        // Tính mốc kết thúc dựa trên mốc bắt đầu đã tìm được
        Calendar endCal = (Calendar) startCal.clone();
        String[] eParts = endStr.split(":");
        endCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(eParts[0]));
        endCal.set(Calendar.MINUTE, Integer.parseInt(eParts[1]));

        if (endCal.before(startCal)) {
            Toast.makeText(requireContext(), "End time must be after start time", Toast.LENGTH_SHORT).show();
            return;
        }

        final EventEntity eventToSave = (isEditMode && currentEvent != null) ? currentEvent : new EventEntity();
        eventToSave.setTitle(title);
        eventToSave.setTeacher(teacher);
        eventToSave.setRoom(room);
        eventToSave.setStartTime(startCal.getTimeInMillis());
        eventToSave.setEndTime(endCal.getTimeInMillis());
        eventToSave.setColor(selectedColorIndex);
        eventToSave.setIsCourse(true);
        eventToSave.setRepeatType("weekly");
        eventToSave.setDayOfWeek(dayOfWeek);
        eventToSave.setDescription("Teacher: " + teacher + "\nRoom: " + room);

        new Thread(() -> {
            if (isEditMode) {
                eventToSave.setId(Math.abs(eventToSave.getId()));
                db.eventDao().updateEvent(eventToSave);
            } else {
                long id = db.eventDao().insertEvent(eventToSave);
                eventToSave.setId((int) id);
            }
            NotificationScheduler.scheduleReminder(requireContext(), eventToSave);
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), isEditMode ? "Updated" : "Added", Toast.LENGTH_SHORT).show();
                dismiss();
            });
        }).start();
    }

    // Hàm phụ trợ: Tìm ngày Calendar khớp với thứ trong tuần (Monday, Tuesday...)
    private Calendar getNextOccurringDayOfWeek(String dayName, int hour, int minute) {
        int targetDay = parseDayOfWeek(dayName);
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
    private int parseDayOfWeek(String dayOfWeek) {
        switch (dayOfWeek.toLowerCase().trim()) {
            case "sunday": return Calendar.SUNDAY;
            case "tuesday": return Calendar.TUESDAY;
            case "wednesday": return Calendar.WEDNESDAY;
            case "thursday": return Calendar.THURSDAY;
            case "friday": return Calendar.FRIDAY;
            case "saturday": return Calendar.SATURDAY;
            default: return Calendar.MONDAY;
        }
    }

    private void setViewsEnabled(boolean enabled) {
        etTitle.setEnabled(enabled);
        etTeacher.setEnabled(enabled);
        etRoom.setEnabled(enabled);
        etStartTime.setEnabled(enabled);
        etEndTime.setEnabled(enabled);
        spinnerDay.setEnabled(enabled);
        btnSelectColor.setEnabled(enabled);
    }

    // Thêm TextWatcher để theo dõi thay đổi dữ liệu
    private final TextWatcher validationWatcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            validateFields(); // Gọi hàm kiểm tra mỗi khi text thay đổi
        }
        @Override public void afterTextChanged(Editable s) {}
    };

    private void validateFields() {
        if (isViewOnly) return;

        boolean isAllFilled = !etTitle.getText().toString().trim().isEmpty() &&
                !etTeacher.getText().toString().trim().isEmpty() &&
                !etRoom.getText().toString().trim().isEmpty() &&
                !etStartTime.getText().toString().trim().isEmpty() &&
                !etEndTime.getText().toString().trim().isEmpty();

        btnSave.setEnabled(isAllFilled);
        btnSave.setAlpha(isAllFilled ? 1.0f : 0.5f);
    }

    private void showTimePicker(EditText editText, boolean isStartTime) {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(requireContext(), (view, hour, minute) -> {
            String time = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
            editText.setText(time);

            if (isStartTime) {
                int endH = (hour + 1) % 24;
                etEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d", endH, minute));
            } else {
                int startH = (hour - 1 < 0) ? 23 : hour - 1;
                etStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d", startH, minute));
            }
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            // Nền mờ
            getDialog().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            getDialog().getWindow().setDimAmount(0.6f);

            // Popup bo góc trong suốt
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(
                    ContextCompat.getColor(requireContext(), R.color.transparent)
            ));

            // Kích thước
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            int screenWidth = displayMetrics.widthPixels;
            double ratio = 0.85;
            int dialogWidth = (int) (screenWidth * ratio);

            getDialog().getWindow().setLayout(dialogWidth,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}

