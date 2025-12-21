package com.mtp.shedule.notification;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.mtp.shedule.MainActivity;
import com.mtp.shedule.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "EVENT_REMINDER_CHANNEL";
    private static final int NOTIFICATION_ID_BASE = 1000;
    private static final String TAG = "ReminderReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "========== onReceive STARTED ==========");

        try {
            // 1. Lấy dữ liệu
            int eventId = intent.getIntExtra("EVENT_ID", -1);
            String eventTitle = intent.getStringExtra("EVENT_TITLE");
            long startTimeMillis = intent.getLongExtra("EVENT_START_TIME", 0);

            Log.d(TAG, "Receiver Fired for ID: " + eventId + " | Title: " + eventTitle);
            Log.d(TAG, "StartTime: " + startTimeMillis);

            if (eventId == -1) {
                Log.e(TAG, " Event ID is -1");
                return;
            }

            if (eventTitle == null || eventTitle.isEmpty()) {
                Log.e(TAG, " Event Title is null or empty");
                return;
            }

            // 2. Tạo Notification Channel
            createNotificationChannel(context);

            // 3. Kiểm tra quyền (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, " KHÔNG CÓ QUYỀN POST_NOTIFICATIONS!");
                    return;
                }
                Log.d(TAG, " Đã có quyền POST_NOTIFICATIONS");
            }

            // 4. Format thời gian
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            String startTimeFormatted = sdf.format(new Date(startTimeMillis));
            Log.d(TAG, "Time formatted: " + startTimeFormatted);

            // 5. Tạo Intent mở app khi nhấn vào notification
            Intent notificationIntent = new Intent(context, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

            int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
            }

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context,
                    eventId,
                    notificationIntent,
                    pendingIntentFlags
            );

            // 6. Lấy âm thanh mặc định
            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

            // 7. Xây dựng Notification
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("🔔 " + eventTitle)
                    .setContentText("Bắt đầu lúc: " + startTimeFormatted)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setSound(soundUri)
                    .setVibrate(new long[]{0, 500, 200, 500})
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent);

            Log.d(TAG, "Builder created successfully");

            // 8. Hiển thị thông báo
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            int notificationId = NOTIFICATION_ID_BASE + eventId;

            notificationManager.notify(notificationId, builder.build());
            Log.i(TAG, " Thông báo ĐÃ ĐƯỢC GỬI cho ID: " + eventId + " (Notification ID: " + notificationId + ")");

        } catch (Exception e) {
            Log.e(TAG, " LỖI NGHIÊM TRỌNG: " + e.getMessage());
            e.printStackTrace();
        }

        Log.d(TAG, "========== onReceive FINISHED ==========");
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(TAG, "Creating Notification Channel...");

            CharSequence name = "Nhắc nhở sự kiện";
            String description = "Thông báo cho các sự kiện sắp diễn ra";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    null
            );

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, " Notification Channel created: " + CHANNEL_ID);
            } else {
                Log.e(TAG, " NotificationManager is null!");
            }
        }
    }
}