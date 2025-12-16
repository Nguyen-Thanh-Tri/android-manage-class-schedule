package com.mtp.shedule;

import static com.mtp.shedule.SelectColorDialog.COLOR_MAPPING_DRAWABLE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
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

public class AddCourseDialog extends DialogFragment {

    EditText etTitle, etTeacher, etRoom, etStartTime, etEndTime;
    Spinner spinnerDay;
    Button btnSave, btnCancel, btnSelectColor;
    private ConnDatabase db;

    private int selectedColorIndex = 0; // Mặc định là Index 0 (Red)
    private static final String ARG_COURSE_ID = "course_id";
    private static final String ARG_TITLE = "title";
    private static final String ARG_TEACHER = "teacher";
    private static final String ARG_ROOM = "room";
    private static final String ARG_START = "start";
    private static final String ARG_END = "end";
    private static final String ARG_DAY = "day";
    private static final String ARG_COLOR = "color";
    private static final String ARG_EVENT_ITEM = "event_item";
    private int courseId = -1;
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


        //update
        if (getArguments() != null && getArguments().containsKey(ARG_EVENT_ITEM)) {
            isEditMode = true;
            currentEvent = (EventEntity) getArguments().getSerializable(ARG_EVENT_ITEM);

            if (currentEvent != null) {
                etTitle.setText(currentEvent.getTitle());
                etTeacher.setText(currentEvent.getTeacher());
                etRoom.setText(currentEvent.getRoom());

                // Format millis sang HH:mm để hiển thị
                etStartTime.setText(currentEvent.getStartTimeFormatted());
                etEndTime.setText(currentEvent.getEndTimeFormatted());

                selectedColorIndex = currentEvent.getColor();

                // Chọn đúng thứ trong spinner
                String day = currentEvent.getDayOfWeek();
                // Chuyển ký tự đầu thành hoa để khớp với list (Monday, Tuesday...)
                if(day != null && !day.isEmpty()) {
                    String formattedDay = day.substring(0, 1).toUpperCase() + day.substring(1).toLowerCase();
                    int spinnerPos = days.indexOf(formattedDay);
                    if (spinnerPos >= 0) spinnerDay.setSelection(spinnerPos);
                }

                btnSave.setText("Update");
            }
        }

        // --- TimePicker cho Start Time ---
        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime));
        // --- TimePicker cho End Time ---
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime));

        btnSave.setOnClickListener(v -> saveCourseAsEvent());


        // Cập nhật màu nút ngay lần đầu mở dialog
        btnSelectColor.setBackgroundResource(COLOR_MAPPING_DRAWABLE[selectedColorIndex]);

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


        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dismiss());
        }

        return view;
    }

    private void saveCourseAsEvent() {
        String title = etTitle.getText().toString().trim();
        String teacher = etTeacher.getText().toString().trim();
        String room = etRoom.getText().toString().trim();
        String startStr = etStartTime.getText().toString().trim();
        String endStr = etEndTime.getText().toString().trim();
        String dayOfWeek = spinnerDay.getSelectedItem().toString();

        if (title.isEmpty() || teacher.isEmpty() || startStr.isEmpty() || endStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        // TÍNH TOÁN TIMESTAMP (Millis)
        // Ta cần tìm ngày gần nhất tương ứng với Thứ đã chọn để làm mốc thời gian
        Calendar startCal = getNextOccurringDayOfWeek(dayOfWeek);
        String[] startParts = startStr.split(":");
        startCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(startParts[0]));
        startCal.set(Calendar.MINUTE, Integer.parseInt(startParts[1]));
        startCal.set(Calendar.SECOND, 0);

        Calendar endCal = (Calendar) startCal.clone();
        String[] endParts = endStr.split(":");
        endCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(endParts[0]));
        endCal.set(Calendar.MINUTE, Integer.parseInt(endParts[1]));
        endCal.set(Calendar.SECOND, 0);

        if (endCal.before(startCal)) {
            Toast.makeText(requireContext(), "End time must be after start time", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo hoặc Cập nhật EventEntity
        final EventEntity eventToSave;
        if (isEditMode && currentEvent != null) {
            eventToSave = currentEvent; // Sử dụng object cũ
        } else {
            eventToSave = new EventEntity(); // Tạo mới
        }

        // Set các thông tin chung
        eventToSave.setTitle(title);
        eventToSave.setTeacher(teacher);
        eventToSave.setRoom(room);
        eventToSave.setStartTime(startCal.getTimeInMillis());
        eventToSave.setEndTime(endCal.getTimeInMillis());
        eventToSave.setColor(selectedColorIndex);

        // QUAN TRỌNG: Đánh dấu đây là Course để Calendar tự repeat
        eventToSave.setIsCourse(true);
        eventToSave.setRepeatType("weekly");
        eventToSave.setDayOfWeek(dayOfWeek); // "Monday", "Tuesday"...
        eventToSave.setDescription("Teacher: " + teacher + "\nRoom: " + room); // Description phụ

        new Thread(() -> {
            if (isEditMode) {
                // ID dương mới update được (xử lý trường hợp ID âm từ Calendar)
                int originalId = Math.abs(eventToSave.getId());
                eventToSave.setId(originalId);

                db.eventDao().updateEvent(eventToSave);
                NotificationScheduler.scheduleReminder(requireContext(), eventToSave);
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Updated successfully", Toast.LENGTH_SHORT).show());
            } else {
                long id = db.eventDao().insertEvent(eventToSave);
                eventToSave.setId((int)id);
                NotificationScheduler.scheduleReminder(requireContext(), eventToSave);
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Course added successfully", Toast.LENGTH_SHORT).show();
                });
            }
            dismiss();
        }).start();
    }

    // Hàm phụ trợ: Tìm ngày Calendar khớp với thứ trong tuần (Monday, Tuesday...)
    private Calendar getNextOccurringDayOfWeek(String dayName) {
        int targetDay = parseDayOfWeek(dayName);
        Calendar cal = Calendar.getInstance();

        // Nếu hôm nay khớp thứ, dùng hôm nay. Nếu không, tìm ngày tiếp theo.
        // Tuy nhiên, để lịch đẹp, ta nên set về tuần hiện tại hoặc tương lai gần.
        while (cal.get(Calendar.DAY_OF_WEEK) != targetDay) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
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

    private void showTimePicker(EditText editText) {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute) -> editText.setText(String.format("%02d:%02d", hourOfDay, minute)),
                c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            // Nền mờ
            getDialog().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            getDialog().getWindow().setDimAmount(0.6f);

            // Popup bo góc trong suốt
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // Kích thước
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            int screenWidth = displayMetrics.widthPixels;
            double ratio = 0.75;
            int dialogWidth = (int) (screenWidth * ratio);

            getDialog().getWindow().setLayout(dialogWidth,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}

