package com.mtp.shedule.notification;

import android.Manifest;
import android.app.ActivityManager;
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

import com.mtp.shedule.AddEventActivity;
import com.mtp.shedule.MainActivity;
import com.mtp.shedule.R;
import com.mtp.shedule.utils.AppSettings;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReminderReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "EVENT_REMINDER_CHANNEL";
    private static final int NOTIFICATION_ID_BASE = 1000;
    private static final String TAG = "ReminderReceiver";

  //  @Override
//    public void onReceive(Context context, Intent intent) {
//        Log.d(TAG, "========== onReceive STARTED ==========");
//
//        try {
//            // 1. Lấy dữ liệu
//            int eventId = intent.getIntExtra("EVENT_ID", -1);
//            String eventTitle = intent.getStringExtra("EVENT_TITLE");
//            long startTimeMillis = intent.getLongExtra("EVENT_START_TIME", 0);
//
//            Log.d(TAG, "Receiver Fired for ID: " + eventId + " | Title: " + eventTitle);
//            Log.d(TAG, "StartTime: " + startTimeMillis);
//
//            if (eventId == -1) {
//                Log.e(TAG, " Event ID is -1");
//                return;
//            }
//
//            if (eventTitle == null || eventTitle.isEmpty()) {
//                Log.e(TAG, " Event Title is null or empty");
//                return;
//            }
//
//            // 2. Tạo Notification Channel
//            createNotificationChannel(context);
//
//            // 3. Kiểm tra quyền (Android 13+)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
//                        != PackageManager.PERMISSION_GRANTED) {
//                    Log.e(TAG, " KHÔNG CÓ QUYỀN POST_NOTIFICATIONS!");
//                    return;
//                }
//                Log.d(TAG, " Đã có quyền POST_NOTIFICATIONS");
//            }
//
//            // 4. Format thời gian
//            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
//            String startTimeFormatted = sdf.format(new Date(startTimeMillis));
//            Log.d(TAG, "Time formatted: " + startTimeFormatted);
//
//            // 5. Tạo Intent mở app khi nhấn vào notification
//            Intent notificationIntent = new Intent(context, MainActivity.class);
//            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//
//            int pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT;
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                pendingIntentFlags |= PendingIntent.FLAG_IMMUTABLE;
//            }
//
//            PendingIntent pendingIntent = PendingIntent.getActivity(
//                    context,
//                    eventId,
//                    notificationIntent,
//                    pendingIntentFlags
//            );
//
//            // 6. Lấy âm thanh mặc định
//            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//
//            // 7. Xây dựng Notification
//            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
//                    .setSmallIcon(R.drawable.ic_notification)
//                    .setContentTitle("🔔 " + eventTitle)
//                    .setContentText("Bắt đầu lúc: " + startTimeFormatted)
//                    .setPriority(NotificationCompat.PRIORITY_HIGH)
//                    .setCategory(NotificationCompat.CATEGORY_ALARM)
//                    .setSound(soundUri)
//                    .setVibrate(new long[]{0, 500, 200, 500})
//                    .setAutoCancel(true)
//                    .setContentIntent(pendingIntent);
//
//            Log.d(TAG, "Builder created successfully");
//
//            // 8. Hiển thị thông báo
//            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
//            int notificationId = NOTIFICATION_ID_BASE + eventId;
//
//            notificationManager.notify(notificationId, builder.build());
//            Log.i(TAG, " Thông báo ĐÃ ĐƯỢC GỬI cho ID: " + eventId + " (Notification ID: " + notificationId + ")");
//
//        } catch (Exception e) {
//            Log.e(TAG, " LỖI NGHIÊM TRỌNG: " + e.getMessage());
//            e.printStackTrace();
//        }
//
//        Log.d(TAG, "========== onReceive FINISHED ==========");
//    }
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "========== onReceive STARTED ==========");

        try {
            // 1. Lấy dữ liệu
            int eventId = intent.getIntExtra("EVENT_ID", -1);
            String eventTitle = intent.getStringExtra("EVENT_TITLE");
            long startTimeMillis = intent.getLongExtra("EVENT_START_TIME", 0);
            int reminderMinutes = intent.getIntExtra("EVENT_REMINDER", 0);

            Log.d(TAG, "Receiver Fired for ID: " + eventId + " | Title: " + eventTitle);
            Log.d(TAG, "StartTime: " + startTimeMillis + " | Reminder: " + reminderMinutes + " minutes before");

            if (eventId == -1) {
                Log.e(TAG, "❌ Event ID is -1");
                return;
            }

            if (eventTitle == null || eventTitle.isEmpty()) {
                Log.e(TAG, "❌ Event Title is null or empty");
                eventTitle = "Sự kiện";
            }

            // 2. PHÂN LOẠI REMINDER
            if (reminderMinutes == 0) {
                // Event đang bắt đầu - KHỞI ĐỘNG FOREGROUND SERVICE VỚI RINGTONE
                startForegroundService(context, eventId, eventTitle, startTimeMillis);
            } else {
                // Reminder trước event - TẠO NOTIFICATION THƯỜNG
                createNormalNotification(context, eventId, eventTitle, startTimeMillis, reminderMinutes);
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ LỖI NGHIÊM TRỌNG: " + e.getMessage());
            e.printStackTrace();
        }

        Log.d(TAG, "========== onReceive FINISHED ==========");
    }

    // THÊM PHƯƠNG THỨC MỚI (sau onReceive):
    private void startForegroundService(Context context, int eventId, String title, long startTime) {
        // Kiểm tra nếu service đang chạy (tránh duplicate)
        if (isMyServiceRunning(context, ReminderForegroundService.class)) {
            Log.d(TAG, "Service is already running for event ID: " + eventId);
            return;
        }

        Intent serviceIntent = new Intent(context, ReminderForegroundService.class);
        serviceIntent.putExtra("EVENT_ID", eventId);
        serviceIntent.putExtra("EVENT_TITLE", title);
        serviceIntent.putExtra("EVENT_START_TIME", startTime);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent);
        } else {
            context.startService(serviceIntent);
        }
        Log.d(TAG, "✅ Started Foreground Service for event ID: " + eventId);
    }

    private void createNormalNotification(Context context, int eventId, String eventTitle, long startTimeMillis, int reminderMinutes) {
        // Tạo Notification Channel (nếu chưa có)
        createNotificationChannel(context);

        // Kiểm tra quyền (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "❌ KHÔNG CÓ QUYỀN POST_NOTIFICATIONS!");
                return;
            }
        }

        // Format thời gian
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String startTimeFormatted = sdf.format(new Date(startTimeMillis));

        // Intent mở ĐÚNG ACTIVITY KHI CLICK
        Intent notificationIntent = new Intent(context, AddEventActivity.class);
        notificationIntent.putExtra("target_event_id", eventId);
        notificationIntent.putExtra("is_view_mode", true);
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

        // Lấy âm thanh từ AppSettings (ringtone user đã chọn)
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        try {
            AppSettings settings = new AppSettings(context);
            Uri customUri = settings.getRingtoneUri();
            if (customUri != null) {
                soundUri = customUri;
            }
        } catch (Exception e) {
            Log.e(TAG, "Cannot get custom ringtone: " + e.getMessage());
        }

        // Tạo thông báo
        String contentText;
        if (reminderMinutes == 0) {
            contentText = "Sự kiện bắt đầu lúc " + startTimeFormatted;
        } else {
            contentText = reminderMinutes + " phút nữa sự kiện bắt đầu lúc " + startTimeFormatted;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("⏰ " + eventTitle)
                .setContentText(contentText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setSound(soundUri)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        // Hiển thị thông báo
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        int notificationId = NOTIFICATION_ID_BASE + eventId;
        notificationManager.notify(notificationId, builder.build());
        Log.i(TAG, "✅ Normal notification sent for event ID: " + eventId);
    }

    // THÊM PHƯƠNG THỨC KIỂM TRA SERVICE ĐANG CHẠY
    private boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) return false;

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
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