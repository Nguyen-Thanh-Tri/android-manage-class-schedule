package com.mtp.shedule;

import static com.mtp.shedule.SelectColorDialog.COLOR_MAPPING_DRAWABLE;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.ExamEntity;

import java.util.Calendar;

public class AddExamDialog extends DialogFragment {
    private EditText etSubject, etDate, etTime, etLocation;
    private Button btnSave, btnCancel, btnSelectColor;
    private int selectedColorIndex = 0; // Mặc định là Index 0 (Red)
    private ConnDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_add_exam, container, false);

        etSubject = view.findViewById(R.id.etExamSubject);
        etDate = view.findViewById(R.id.etExamDate);
        etTime = view.findViewById(R.id.etExamTime);
        etLocation = view.findViewById(R.id.etExamLocation);
        btnSave = view.findViewById(R.id.btnSaveExam);
        btnCancel = view.findViewById(R.id.btnCancelExam);
        btnSelectColor = view.findViewById(R.id.btnSelectColor);

        db = ConnDatabase.getInstance(requireContext());

        // Pre-fill fields if editing
        Bundle args = getArguments();
        if (args != null) {
            etSubject.setText(args.getString("subject", ""));
            etDate.setText(args.getString("date", ""));
            etTime.setText(args.getString("time", ""));
            etLocation.setText(args.getString("location", ""));
            btnSave.setText("Update Exam");
        }

        etDate.setOnClickListener(v -> showDatePicker(etDate));
        etTime.setOnClickListener(v -> showTimePicker(etTime));

        // Cập nhật màu nút ngay lần đầu mở dialog
        btnSelectColor.setBackgroundResource(COLOR_MAPPING_DRAWABLE[selectedColorIndex]);

        btnSelectColor.setOnClickListener(v ->{
            SelectColorDialog dialog = new SelectColorDialog();

            // Truyền mã màu INT để SelectColorDialog đánh dấu ô màu đúng
            dialog.setSelectedColorIndex(selectedColorIndex);

            dialog.setOnColorSelectedListener(colorIndex -> {

                selectedColorIndex = colorIndex;
                btnSelectColor.setBackgroundResource(COLOR_MAPPING_DRAWABLE[selectedColorIndex]);

                // Lưu ý: Nếu bạn cần cập nhật selectedColor INT để mở Dialog lại đúng màu,
                // bạn cần thêm logic ánh xạ ngược từ Drawable ID sang Color INT ở đây.
                // (Tạm thời bỏ qua nếu không cần tính năng đó)
            });
            dialog.show(getParentFragmentManager(), "ColorDialog");
        });

        btnSave.setOnClickListener(v -> {
            if (args != null && args.containsKey("id")) {
                updateExam(args.getInt("id"));
            } else {
                saveExam();
            }
        });

        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dismiss());
        }

        return view;
    }

    private void saveExam() {
        String subject = etSubject.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        if (subject.isEmpty() || date.isEmpty() || time.isEmpty() || location.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedColorIndex < 0 || selectedColorIndex >= COLOR_MAPPING_DRAWABLE.length) {
            Toast.makeText(requireContext(), "Please select a color", Toast.LENGTH_SHORT).show();
            return;
        }

        ExamEntity exam = new ExamEntity(subject, date, time, location);
        exam.setColor(selectedColorIndex);

        new Thread(() -> {
            db.examDao().insert(exam);
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Exam added successfully", Toast.LENGTH_SHORT).show();
                dismiss();
            });
        }).start();
    }

    private void updateExam(int id) {
        String subject = etSubject.getText().toString().trim();
        String date = etDate.getText().toString().trim();
        String time = etTime.getText().toString().trim();
        String location = etLocation.getText().toString().trim();

        if (subject.isEmpty() || date.isEmpty() || time.isEmpty() || location.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedColorIndex < 0 || selectedColorIndex >= COLOR_MAPPING_DRAWABLE.length) {
            Toast.makeText(requireContext(), "Please select a color", Toast.LENGTH_SHORT).show();
            return;
        }

        ExamEntity exam = new ExamEntity(subject, date, time, location);
        exam.setId(id);
        exam.setColor(selectedColorIndex);

        new Thread(() -> {
            db.examDao().update(exam);
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), "Exam updated successfully", Toast.LENGTH_SHORT).show();
                dismiss();
            });
        }).start();
    }

    private void showDatePicker(EditText editText) {
        Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                (view, year1, month1, dayOfMonth) -> editText.setText(String.format("%04d-%02d-%02d", year1, month1 + 1, dayOfMonth)),
                year, month, day);
        datePicker.show();
    }

    private void showTimePicker(EditText editText) {
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
            // Dim background
            getDialog().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            getDialog().getWindow().setDimAmount(0.6f);

            // Rounded transparent popup
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            // Size
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            int screenWidth = displayMetrics.widthPixels;
            double ratio = 0.75;
            int dialogWidth = (int) (screenWidth * ratio);

            getDialog().getWindow().setLayout(dialogWidth,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
