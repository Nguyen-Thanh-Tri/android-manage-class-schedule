package com.mtp.shedule.notification;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.mtp.shedule.MainActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReminderForegroundService extends Service {
    private static final String TAG = "ReminderFGService";
    private static final String EVENT_CHANNEL_ID = "EVENT_REMINDER_CHANNEL";
    private static final String FG_CHANNEL_ID = "FOREGROUND_SERVICE_CHANNEL";
    private static final int FG_NOTIFICATION_ID = 9999;
    private static final long RINGTONE_DURATION = 60 * 1000; // 60 giây = 1 phút

    private Handler handler;
    private Runnable stopRingtoneRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "========== SERVICE CREATED ==========");
        createChannels();
        handler = new Handler(Looper.getMainLooper());
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "========== SERVICE STARTED ==========");
        Log.e(TAG, "Time: " + System.currentTimeMillis());

        // B1: Hiển thị Foreground Notification
        if (Build.VERSION.SDK_INT >= 34) { // Android 14+
            startForeground(
                    FG_NOTIFICATION_ID,
                    createForegroundNotification(),
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE
            );
        } else {
            startForeground(FG_NOTIFICATION_ID, createForegroundNotification());
        }
        Log.e(TAG, "Foreground notification displayed");

        try {
            // Lấy dữ liệu
            int eventId = intent.getIntExtra("EVENT_ID", -1);
            String eventTitle = intent.getStringExtra("EVENT_TITLE");
            long startTimeMillis = intent.getLongExtra("EVENT_START_TIME", 0);

            Log.e(TAG, "EventID: " + eventId);
            Log.e(TAG, "Title: " + eventTitle);

            if (eventId == -1 || eventTitle == null || eventTitle.isEmpty()) {
                Log.e(TAG, "Invalid data");
                stopForeground(true);
                stopSelf();
                return START_NOT_STICKY;
            }

            // B2: Phát nhạc chuông
            RingtonePlayer.play(this);
            Log.e(TAG, "Ringtone started");

            // B3: Hiển thị Event Notification với nút Huỷ
            int notificationId = 1000 + eventId;
            showEventNotification(eventId, eventTitle, startTimeMillis, notificationId);

            // B4: Tự động dừng nhạc sau 1 phút
            stopRingtoneRunnable = new Runnable() {
                @Override
                public void run() {
                    Log.e(TAG, "⏰ Auto-stop ringtone after 1 minute");
                    RingtonePlayer.stop();

                    // Cập nhật notification (xoá nút Huỷ)
                    updateNotificationAfterStop(notificationId, eventTitle, startTimeMillis);

                    // Dừng service
                    stopForeground(true);
                    stopSelf();
                }
            };
            handler.postDelayed(stopRingtoneRunnable, RINGTONE_DURATION);

            Log.e(TAG, "Ringtone will auto-stop in 60 seconds");

        } catch (Exception e) {
            Log.e(TAG, "ERROR: " + e.getMessage());
            e.printStackTrace();
            RingtonePlayer.stop();
            stopForeground(true);
            stopSelf();
        }

        Log.e(TAG, "========== SERVICE SETUP COMPLETED ==========");
        return START_NOT_STICKY;
    }

    private void showEventNotification(int eventId, String eventTitle, long startTimeMillis, int notificationId) {
        // Format thời gian
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String startTimeFormatted = sdf.format(new Date(startTimeMillis));

        // Intent mở app
        Intent openAppIntent = new Intent(this, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                this,
                eventId,
                openAppIntent,
                flags
        );

        // Intent cho nút Huỷ
        Intent stopIntent = new Intent(this, StopRingtoneReceiver.class);
        stopIntent.setAction(StopRingtoneReceiver.ACTION_STOP_RINGTONE);
        stopIntent.putExtra("NOTIFICATION_ID", notificationId);

        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(
                this,
                eventId + 20000, // Request code khác để tránh trùng
                stopIntent,
                flags
        );

        // Build notification với nút Huỷ
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, EVENT_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("🔔 " + eventTitle)
                .setContentText("Bắt đầu lúc: " + startTimeFormatted)
                .setSubText("🎵 Đang phát nhạc chuông...")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(true) // Không thể vuốt xoá
                .setAutoCancel(false) // Không tự động xoá khi click
                .setContentIntent(openAppPendingIntent)
                .setSound(null) // Tắt sound mặc định vì dùng RingtonePlayer
                .setVibrate(new long[]{0, 500, 200, 500})
                // THÊM NÚT HUỶ
                .addAction(
                        android.R.drawable.ic_delete,
                        "Huỷ ⏹️",
                        stopPendingIntent
                );

        // Hiển thị
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            try {
                notificationManager.notify(notificationId, builder.build());
            } catch (SecurityException e) {
                Log.e(TAG, "Oppo Security Restriction: " + e.getMessage());
            }
            Log.e(TAG, "EVENT NOTIFICATION WITH STOP BUTTON SENT! ID: " + notificationId );
        }
    }

    private void updateNotificationAfterStop(int notificationId, String eventTitle, long startTimeMillis) {
        // Format thời gian
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String startTimeFormatted = sdf.format(new Date(startTimeMillis));

        // Intent mở app
        Intent openAppIntent = new Intent(this, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                this,
                notificationId,
                openAppIntent,
                flags
        );

        // Build notification KHÔNG có nút Huỷ
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, EVENT_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("🔔 " + eventTitle)
                .setContentText("Bắt đầu lúc: " + startTimeFormatted)
                .setSubText("⏹️ Nhạc chuông đã dừng")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setOngoing(false) // Có thể vuốt xoá
                .setAutoCancel(true) // Tự động xoá khi click
                .setContentIntent(openAppPendingIntent);

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            try {
                notificationManager.notify(notificationId, builder.build());
            } catch (SecurityException e) {
                Log.e(TAG, "Oppo Security Restriction: " + e.getMessage());
            }
            Log.e(TAG, "Notification updated (ringtone stopped)");
        }
    }

    private Notification createForegroundNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, FG_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Đang hiển thị nhắc nhở")
                .setContentText("Xử lý thông báo sự kiện...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);

        return builder.build();
    }

    private void createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager == null) {
                Log.e(TAG, "NotificationManager is null!");
                return;
            }

            // Channel cho Foreground Service
            NotificationChannel fgChannel = new NotificationChannel(
                    FG_CHANNEL_ID,
                    "Dịch vụ nhắc nhở",
                    NotificationManager.IMPORTANCE_LOW
            );
            fgChannel.setDescription("Dịch vụ chạy nền để hiển thị nhắc nhở");
            fgChannel.setShowBadge(false);
            fgChannel.setSound(null, null); // Tắt âm thanh
            manager.createNotificationChannel(fgChannel);

            // Channel cho Event Notification
            NotificationChannel eventChannel = new NotificationChannel(
                    EVENT_CHANNEL_ID,
                    "Nhắc nhở sự kiện",
                    NotificationManager.IMPORTANCE_HIGH
            );
            eventChannel.setDescription("Thông báo cho các sự kiện sắp diễn ra");
            eventChannel.enableLights(true);
            eventChannel.enableVibration(true);
            eventChannel.setSound(null, null); // Tắt âm thanh mặc định vì dùng RingtonePlayer
            manager.createNotificationChannel(eventChannel);

            Log.e(TAG, "Channels created");
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        // 1. Hủy Handler ngay lập tức để dừng đếm ngược 1 phút
        if (handler != null && stopRingtoneRunnable != null) {
            handler.removeCallbacks(stopRingtoneRunnable);
            Log.e(TAG, "Successfully removed stopRingtoneRunnable");
        }

        // 2. Dừng và giải phóng MediaPlayer
        RingtonePlayer.stop();

        // 3. Xóa Notification của chính nó (Short Service)
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.cancel(FG_NOTIFICATION_ID);

        super.onDestroy();
        Log.e(TAG, "========== SERVICE COMPLETELY DESTROYED ==========");
    }
}