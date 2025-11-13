package com.mtp.shedule;

import static com.mtp.shedule.SelectColorDialog.COLOR_MAPPING_DRAWABLE;

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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.TeacherEntity;

public class AddTeacherDialog extends DialogFragment {

    private EditText etName, etPosition, etPhone, etEmail;
    private Button btnSelectColor, btnSave, btnCancel;
    private int selectedColorIndex = 0;
    private ConnDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_add_teacher, container, false);

        etName = view.findViewById(R.id.etName);
        etPosition = view.findViewById(R.id.etPosition);
        etPhone = view.findViewById(R.id.etPhone);
        etEmail = view.findViewById(R.id.etEmail);
        btnSelectColor = view.findViewById(R.id.btnSelectColor);
        btnSave = view.findViewById(R.id.btnSave);
        btnCancel = view.findViewById(R.id.btnCancel);

        db = ConnDatabase.getInstance(requireContext());

        // Cập nhật nút chọn màu lần đầu
        if (selectedColorIndex >= 0 && selectedColorIndex < COLOR_MAPPING_DRAWABLE.length)
            btnSelectColor.setBackgroundResource(COLOR_MAPPING_DRAWABLE[selectedColorIndex]);

        btnSelectColor.setOnClickListener(v -> {
            SelectColorDialog dialog = new SelectColorDialog();
            dialog.setSelectedColorIndex(selectedColorIndex);
            dialog.setOnColorSelectedListener(colorIndex -> {
                if (colorIndex >= 0 && colorIndex < COLOR_MAPPING_DRAWABLE.length) {
                    selectedColorIndex = colorIndex;
                    btnSelectColor.setBackgroundResource(COLOR_MAPPING_DRAWABLE[selectedColorIndex]);
                }
            });
            dialog.show(getParentFragmentManager(), "SelectColorDialog");
        });

        btnSave.setOnClickListener(v -> {
            Bundle args = getArguments();
            if (args != null && args.containsKey("id")) {
                updateTeacher(args.getInt("id"));
            } else {
                saveTeacher();
            }
        });

        btnCancel.setOnClickListener(v -> dismiss());

        // Pre-fill nếu edit
        Bundle args = getArguments();
        if (args != null) {
            etName.setText(args.getString("name", ""));
            etPosition.setText(args.getString("position", ""));
            etPhone.setText(args.getString("phone", ""));
            etEmail.setText(args.getString("email", ""));
            selectedColorIndex = args.getInt("colorIndex", 0);
            if (selectedColorIndex >= 0 && selectedColorIndex < COLOR_MAPPING_DRAWABLE.length)
                btnSelectColor.setBackgroundResource(COLOR_MAPPING_DRAWABLE[selectedColorIndex]);
            btnSave.setText("Update Teacher");
        }

        return view;
    }

    private void saveTeacher() {
        String name = etName.getText().toString().trim();
        String position = etPosition.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        TeacherEntity teacher = new TeacherEntity(name, position, phone, email);
        teacher.setColor(selectedColorIndex);

        new Thread(() -> {
            db.teacherDao().insertTeacher(teacher);
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Teacher added successfully", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().setFragmentResult("teacher_added", new Bundle());
                    dismiss();
                });
            }
        }).start();
    }

    private void updateTeacher(int id) {
        String name = etName.getText().toString().trim();
        String position = etPosition.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String email = etEmail.getText().toString().trim();

        if (name.isEmpty() || phone.isEmpty() || email.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all required fields", Toast.LENGTH_SHORT).show();
            return;
        }

        TeacherEntity teacher = new TeacherEntity(name, position, phone, email);
        teacher.setId(id);
        teacher.setColor(selectedColorIndex);

        new Thread(() -> {
            db.teacherDao().updateTeacher(teacher);
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), "Teacher updated successfully", Toast.LENGTH_SHORT).show();
                    getParentFragmentManager().setFragmentResult("teacher_added", new Bundle());
                    dismiss();
                });
            }
        }).start();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            getDialog().getWindow().setDimAmount(0.6f);
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            DisplayMetrics metrics = getResources().getDisplayMetrics();
            int width = (int) (metrics.widthPixels * 0.75);
            getDialog().getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
