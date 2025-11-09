package com.mtp.shedule;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

public class NotificationSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_settings);

        Button btnEnableNotifications = findViewById(R.id.btnEnableNotifications);
        Button btnTestNotification = findViewById(R.id.btnTestNotification);

        // Tạo notification channel
        NotificationHelper.createNotificationChannel(this);

//        btnEnableNotifications.setOnClickListener(v -> {
//            NotificationHelper.scheduleAllClassNotifications(this);
//            Toast.makeText(this, "Đã bật thông báo cho tất cả môn học", Toast.LENGTH_LONG).show();
//        });
        ///
        btnEnableNotifications.setOnClickListener(v -> {
            // HIỆN DIALOG XÁC NHẬN
            DatabaseHelper db = new DatabaseHelper(this);
            int totalCourses = db.getAllCourses().size();

            new AlertDialog.Builder(this)
                    .setTitle("Bật thông báo")
                    .setMessage("Bật thông báo cho tất cả " + totalCourses + " môn học?\n\nThông báo sẽ hiện 2 tiếng trước khi môn học bắt đầu.")
                    .setPositiveButton("Bật ngay", (dialog, which) -> {
                        // LÊN LỊCH VÀ NHẬN KẾT QUẢ
                        int[] results = NotificationHelper.scheduleAllClassNotifications(this);
                        int scheduled = results[0];
                        int skipped = results[1];

                        // HIỆN KẾT QUẢ CHI TIẾT
                        String message = "✅ Đã lên lịch: " + scheduled + " môn học";
                        if (skipped > 0) {
                            message += "\n❌ Bỏ qua: " + skipped + " môn học (thiếu thời gian)";
                        }

                        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        });
        ///

        btnTestNotification.setOnClickListener(v -> {
            // 1. HIỆN NOTIFICATION TEST NGAY LẬP TỨC
            showTestNotification();

            // 2. HIỆN TOAST THÔNG BÁO
            Toast.makeText(this, "Đã gửi thông báo test!", Toast.LENGTH_SHORT).show();

            // 3. TỰ ĐỘNG QUAY VỀ MÀN HÌNH CHÍNH SAU 1 GIÂY
            new android.os.Handler().postDelayed(() -> {
                Intent intent = new Intent(this, MainActivity.class);
                startActivity(intent);
                finish();
            }, 1000); // Chờ 1 giây để user thấy Toast
        });
    }

    // METHOD PHẢI ĐẶT NGOÀI onCreate() - ĐÂY LÀ LỖI CHÍNH
    private void showTestNotification() {
        String title = "Android Development - TEST";
        String teacher = "Thầy Google";
        String room = "701";
        String time = "14:00";

        String notificationText = "Giảng viên: " + teacher + " - Phòng: " + room + " - Bắt đầu: " + time;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "CLASS_SCHEDULE_CHANNEL")
                .setSmallIcon(R.drawable.ic_time)
                .setContentTitle("[TEST] Sắp có tiết học: " + title)
                .setContentText(notificationText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(9999, builder.build()); // ID cố định để dễ test
    }
}