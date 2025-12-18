package com.mtp.shedule;

import static com.mtp.shedule.SelectColorDialog.COLOR_MAPPING_DRAWABLE;

import android.content.Context;
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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.TeacherEntity;

public class AddTeacherDialog extends DialogFragment {

    private EditText etName, etPosition, etPhone, etEmail;
    private Button btnSelectColor, btnSave, btnCancel;
    private int selectedColorIndex = 0;
    private ConnDatabase db;
    private TeacherEntity currentTeacher;
    private boolean isViewOnly = false;

    public static AddTeacherDialog newInstance(TeacherEntity teacher) {
        AddTeacherDialog dialog = new AddTeacherDialog();
        Bundle args = new Bundle();
        args.putSerializable("teacher_data", teacher);
        dialog.setArguments(args);
        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_add_teacher, container, false);
        db = ConnDatabase.getInstance(requireContext());

        etName = v.findViewById(R.id.etName);
        etPosition = v.findViewById(R.id.etPosition);
        etPhone = v.findViewById(R.id.etPhone);
        etEmail = v.findViewById(R.id.etEmail);
        btnSelectColor = v.findViewById(R.id.btnSelectColor);
        btnSave = v.findViewById(R.id.btnSave);
        btnCancel = v.findViewById(R.id.btnCancel);

        // Đăng ký TextWatcher
        etName.addTextChangedListener(watcher);
        etPhone.addTextChangedListener(watcher);
        etEmail.addTextChangedListener(watcher);

        if (getArguments() != null) {
            currentTeacher = (TeacherEntity) getArguments().getSerializable("teacher_data");
            isViewOnly = true;
            fillData();
        }

        updateUIByMode();
        setupColorPicker();

        return v;
    }

    private void fillData() {
        etName.setText(currentTeacher.getName());
        etPosition.setText(currentTeacher.getPosition());
        etPhone.setText(currentTeacher.getPhone());
        etEmail.setText(currentTeacher.getEmail());
        selectedColorIndex = currentTeacher.getColor();
        btnSelectColor.setBackgroundResource(COLOR_MAPPING_DRAWABLE[selectedColorIndex]);
    }

    private void updateUIByMode() {
        boolean canEdit = !isViewOnly;
        etName.setEnabled(canEdit);
        etPosition.setEnabled(canEdit);
        etPhone.setEnabled(canEdit);
        etEmail.setEnabled(canEdit);
        btnSelectColor.setEnabled(canEdit);

        // Nút Cancel luôn đóng Dialog theo yêu cầu của bạn
        btnCancel.setOnClickListener(v -> {
            hideKeyboard();
            dismiss();
        });

        if (isViewOnly) {
            btnSave.setText("Update");
            btnCancel.setText("Delete");
            btnSave.setEnabled(true); btnSave.setAlpha(1.0f);
            btnSave.setOnClickListener(v -> { isViewOnly = false; updateUIByMode(); });
            btnCancel.setOnClickListener(v -> deleteAction());
        } else {
            btnSave.setText(currentTeacher == null ? "Save" : "Confirm Update");
            btnCancel.setText("Cancel");
            btnSave.setOnClickListener(v -> saveAction());
            validateFields();
        }
    }
    // Hàm ẩn bàn phím
    private void hideKeyboard() {
        View view = getDialog() != null ? getDialog().getCurrentFocus() : null;
        if (view == null) view = etName; // Fallback
        InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    private final TextWatcher watcher = new TextWatcher() {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) { validateFields(); }
        @Override public void afterTextChanged(Editable s) {}
    };

    private void validateFields() {
        if (isViewOnly) return;
        boolean isValid = !etName.getText().toString().trim().isEmpty() &&
                !etPhone.getText().toString().trim().isEmpty() &&
                !etEmail.getText().toString().trim().isEmpty();
        btnSave.setEnabled(isValid);
        btnSave.setAlpha(isValid ? 1.0f : 0.5f);
    }

    private void saveAction() {
        new Thread(() -> {
            TeacherEntity t = (currentTeacher == null) ? new TeacherEntity() : currentTeacher;
            t.setName(etName.getText().toString().trim());
            t.setPosition(etPosition.getText().toString().trim());
            t.setPhone(etPhone.getText().toString().trim());
            t.setEmail(etEmail.getText().toString().trim());
            t.setColor(selectedColorIndex);

            if (currentTeacher == null) db.teacherDao().insertTeacher(t);
            else db.teacherDao().updateTeacher(t);

            requireActivity().runOnUiThread(this::dismiss);
        }).start();
    }

    private void deleteAction() {
        new Thread(() -> {
            db.teacherDao().deleteTeacher(currentTeacher);
            requireActivity().runOnUiThread(this::dismiss);
        }).start();
    }

    private void setupColorPicker() {
        btnSelectColor.setOnClickListener(v -> {
            SelectColorDialog dialog = new SelectColorDialog();
            dialog.setSelectedColorIndex(selectedColorIndex);
            dialog.setOnColorSelectedListener(idx -> {
                selectedColorIndex = idx;
                btnSelectColor.setBackgroundResource(COLOR_MAPPING_DRAWABLE[idx]);
            });
            dialog.show(getParentFragmentManager(), "color");
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(ContextCompat.getColor(requireContext(), R.color.transparent)));
            DisplayMetrics dm = getResources().getDisplayMetrics();
            getDialog().getWindow().setLayout((int)(dm.widthPixels * 0.85), WindowManager.LayoutParams.WRAP_CONTENT);
        }
    }
}