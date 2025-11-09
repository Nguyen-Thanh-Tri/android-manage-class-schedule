package com.mtp.shedule;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import android.app.TimePickerDialog;
import android.content.res.ColorStateList;
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
import com.mtp.shedule.entity.CourseEntity;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class AddCourseDialog extends DialogFragment {

    EditText etTitle, etTeacher, etRoom, etStartTime, etEndTime;
    Spinner spinnerDay;
    Button btnSave, btnCancel, btnSelectColor;
    private ConnDatabase db;

    private int selectedColor = Color.parseColor("#4285F4");
    private int selectedColorId = R.drawable.gradient_bg_red;

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


        // --- TimePicker cho Start Time ---
        etStartTime.setOnClickListener(v -> showTimePicker(etStartTime));
        // --- TimePicker cho End Time ---
        etEndTime.setOnClickListener(v -> showTimePicker(etEndTime));

        btnSave.setOnClickListener(v -> saveCourse());


        // Cập nhật màu nút ngay lần đầu mở dialog
        btnSelectColor.setBackgroundResource(selectedColorId);

        btnSelectColor.setOnClickListener(v ->{
            SelectColorDialog dialog = new SelectColorDialog();

            // Truyền mã màu INT để SelectColorDialog đánh dấu ô màu đúng
            dialog.setSelectedColor(selectedColor);

            dialog.setOnColorSelectedListener(drawableResId -> {
                // Cập nhật Drawable ID
                selectedColorId = drawableResId;

                // Cập nhật màu nút (Nếu gradient, dùng setBackgroundResource)
                btnSelectColor.setBackgroundResource(selectedColorId);

                // Lưu ý: Nếu bạn cần cập nhật selectedColor INT để mở Dialog lại đúng màu,
                // bạn cần thêm logic ánh xạ ngược từ Drawable ID sang Color INT ở đây.
                // (Tạm thời bỏ qua nếu không cần tính năng đó)
            });
            dialog.show(getParentFragmentManager(), "ColorDialog");
        });


        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dismiss());
        }

        return view;
    }

    private void saveCourse(){
        String title = etTitle.getText().toString().trim();
        String teacher = etTeacher.getText().toString().trim();
        String room = etRoom.getText().toString().trim();
        String start = etStartTime.getText().toString().trim();
        String end = etEndTime.getText().toString().trim();
        String day = spinnerDay.getSelectedItem().toString();

        if (title.isEmpty() || teacher.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        CourseEntity course = new CourseEntity(title, teacher, room, start, end, day);

        // 4. LƯU ID DRAWABLE VÀO ENTITY (Sử dụng phương thức setter mới)
        course.setColor(selectedColorId);

        new Thread(() -> {
            db.courseDao().insert(course);

            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Course added successfully", Toast.LENGTH_SHORT).show();
                dismiss();
            });
        }).start();
    }

    private void showTimePicker(EditText editText){
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        TimePickerDialog timePicker = new TimePickerDialog(requireContext(),
                (view, hourOfDay, minute1) -> editText.setText(String.format("%02d:%02d", hourOfDay, minute1)),
                hour, minute, true);
        timePicker.show();
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

