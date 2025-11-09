package com.mtp.shedule;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.TeacherEntity;

public class AddTeacherActivity extends AppCompatActivity {

    private EditText etName, etPost, etPhone, etEmail;
    Button btnSelectColor, btnCancel, btnSave;
    private int selectedColorResId = R.drawable.gradient_bg_orange;
    private ConnDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_teacher);

        db = ConnDatabase.getInstance(this);

        etName = findViewById(R.id.etName);
        etPost = findViewById(R.id.etPost);
        etPhone = findViewById(R.id.etPhone);
        etEmail = findViewById(R.id.etEmail);
        btnSelectColor = findViewById(R.id.btnSelectColor);
        btnCancel = findViewById(R.id.btnCancel);
        btnSave = findViewById(R.id.btnSave);

        btnSelectColor.setOnClickListener(v -> showColorPickerDialog());
        btnCancel.setOnClickListener(v -> finish());

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String position = etPost.getText().toString().trim();
            String phone = etPhone.getText().toString().trim();
            String email = etEmail.getText().toString().trim();

            if (name.isEmpty() || phone.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show();
                return;
            }

            TeacherEntity teacher = new TeacherEntity(name, position, phone, email, selectedColorResId);

            new Thread(() -> {
                db.teacherDao().insertTeacher(teacher);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Teacher added successfully", Toast.LENGTH_SHORT).show();
//                    startActivity(new Intent(this, MainActivity.class));
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("added", true);
                    setResult(Activity.RESULT_OK, resultIntent);
                    finish();
                });
            }).start();
        });
    }

    private void showColorPickerDialog() {
        String[] colorOptions = {"Orange", "Blue", "Green", "Red"};
        int[] colorResIds = {
                R.drawable.gradient_bg_orange,
                R.drawable.gradient_bg_blue,
                R.drawable.gradient_bg_green,
                R.drawable.gradient_bg_red
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Card Color")
                .setItems(colorOptions, (dialog, which) -> {
                    selectedColorResId = colorResIds[which];
                    btnSelectColor.setBackgroundResource(selectedColorResId);
                })
                .show();
    }
}
