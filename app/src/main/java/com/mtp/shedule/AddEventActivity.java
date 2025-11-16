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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.internal.TextWatcherAdapter;
import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.EventEntity;

import java.util.Calendar;
import java.util.Locale;

public class AddEventActivity extends AppCompatActivity {

    EditText etTitle, etDescription;
    Button btnStartDate, btnStartTime, btnEndDate, btnEndTime, btnColorPicker, btnSave, btnCancel;
    private ConnDatabase db;
    // Calendar objects for picking date & time
    Calendar startCal = Calendar.getInstance();
    Calendar endCal = Calendar.getInstance();
    private int selectedColorIndex = 0; // Mặc định là Index 0 (Red)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        db = ConnDatabase.getInstance(this);

        endCal.add(Calendar.HOUR_OF_DAY, 1);

        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        btnStartDate = findViewById(R.id.btnStartDate);
        btnStartTime = findViewById(R.id.btnStartTime);
        btnEndDate = findViewById(R.id.btnEndDate);
        btnEndTime = findViewById(R.id.btnEndTime);
        btnColorPicker = findViewById(R.id.btnColorPicker);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);

        etTitle.addTextChangedListener(watcher);
        etDescription.addTextChangedListener(watcher);

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

                    // 🚀Tự động cập nhật EndCal sau +1h
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

    //Save Event
    private void saveEvent() {
        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        long start = startCal.getTimeInMillis();
        long end = endCal.getTimeInMillis();

        if (title.isEmpty() || desc.isEmpty()) {
            Toast.makeText(this, "Use your finger and punch on me, please", Toast.LENGTH_SHORT).show();
            return;
        }

        if (endCal.before(startCal)) {
            Toast.makeText(this, "timeend sooner timestart? are you the Alien.", Toast.LENGTH_SHORT).show();
            return;
        }

        EventEntity event = new EventEntity(title,desc,start,end);
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

        Toast.makeText(this, "Event saved!", Toast.LENGTH_SHORT).show();
        
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
        btnSave.setEnabled(enable);
    }
}