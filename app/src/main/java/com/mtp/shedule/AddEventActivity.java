package com.mtp.shedule;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.graphics.Color;
import android.media.metrics.Event;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.EventEntity;

import java.util.Calendar;

public class AddEventActivity extends AppCompatActivity {

    EditText etTitle, etDescription;
    Button btnStartDate, btnStartTime, btnEndDate, btnEndTime, btnColorPicker, btnSave, btnCancel;
    Spinner spinnerTimezone, spinnerRepeat;
    private ConnDatabase db;
    // Calendar objects for picking date & time
    Calendar startCal = Calendar.getInstance();
    Calendar endCal = Calendar.getInstance();

    int selectedColor = Color.BLUE; // default

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_event);

        db = ConnDatabase.getInstance(this);

        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        btnStartDate = findViewById(R.id.btnStartDate);
        btnStartTime = findViewById(R.id.btnStartTime);
        btnEndDate = findViewById(R.id.btnEndDate);
        btnEndTime = findViewById(R.id.btnEndTime);
        btnColorPicker = findViewById(R.id.btnColorPicker);
        btnSave = findViewById(R.id.btnSave);
        btnCancel = findViewById(R.id.btnCancel);
        spinnerTimezone = findViewById(R.id.spinnerTimezone);
        spinnerRepeat = findViewById(R.id.spinnerRepeat);

        ArrayAdapter<CharSequence> repeatAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.repeat_options,
                android.R.layout.simple_spinner_item
            );
        repeatAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRepeat.setAdapter(repeatAdapter);

        ArrayAdapter<CharSequence> timezoneAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.timezone_options,
                android.R.layout.simple_spinner_item
            );
        timezoneAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTimezone.setAdapter(timezoneAdapter);


        //DateTime Pickers
        btnStartDate.setOnClickListener(v -> pickDate(startCal, btnStartDate));
        btnStartTime.setOnClickListener(v -> pickTime(startCal, btnStartTime));

        btnEndDate.setOnClickListener(v -> pickDate(endCal, btnEndDate));
        btnEndTime.setOnClickListener(v -> pickTime(endCal, btnEndTime));

        btnColorPicker.setOnClickListener(v -> chooseColor());

        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> saveEvent());
    }

    // Pick Date
    @SuppressLint("SetTextI18n")
    private void pickDate(Calendar cal, Button btn) {
        new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    cal.set(Calendar.YEAR, year);
                    cal.set(Calendar.MONTH, month);
                    cal.set(Calendar.DAY_OF_MONTH, day);
                    btn.setText(day + "/" + (month + 1) + "/" + year);
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    // Pick Time
    @SuppressLint("DefaultLocale")
    private void pickTime(Calendar cal, Button btn) {
        new TimePickerDialog(
                this,
                (view, hour, minute) -> {
                    cal.set(Calendar.HOUR_OF_DAY, hour);
                    cal.set(Calendar.MINUTE, minute);
                    btn.setText(String.format("%02d:%02d", hour, minute));
                },
                cal.get(Calendar.HOUR_OF_DAY),
                cal.get(Calendar.MINUTE),
                true
        ).show();
    }

    //Pick Color
    private void chooseColor() {
        // bạn có thể thay bằng dialog chọn màu tùy ý
        selectedColor = Color.RED;
        btnColorPicker.setBackgroundColor(selectedColor);
        Toast.makeText(this, "Color selected!", Toast.LENGTH_SHORT).show();
    }

    //Save Event
    private void saveEvent() {
        String title = etTitle.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();
        long start = startCal.getTimeInMillis();
        long end = endCal.getTimeInMillis();
        String timezone = spinnerTimezone.getSelectedItem().toString();
        String repeat = spinnerRepeat.getSelectedItem().toString();

        if (title.isEmpty() || desc.isEmpty() || repeat.isEmpty() || timezone.isEmpty() || start == 0 || end == 0) {
            Toast.makeText(this, "Please enter title", Toast.LENGTH_SHORT).show();
            return;
        }

        EventEntity event = new EventEntity(title,desc,start,end,timezone,repeat);
        event.setColor(1);

        new Thread(() -> {
            db.eventDao().insertEvent(event);
        }).start();

        Toast.makeText(this, "Event saved!", Toast.LENGTH_SHORT).show();
        finish();
    }
}