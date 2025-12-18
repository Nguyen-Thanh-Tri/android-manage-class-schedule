package com.mtp.shedule.notification;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.mtp.shedule.AddEventActivity;
import com.mtp.shedule.MainActivity;
import com.mtp.shedule.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReminderForegroundService extends Service {
    private static final String TAG = "ReminderFGService";

    // Chỉ dùng 1 Channel ID duy nhất cho đơn giản và hiệu quả
    private static final String CHANNEL_ID = "EVENT_REMINDER_CHANNEL";

    // ID cố định cho Foreground Service (để nó dính trên thanh thông báo)
    private static final int NOTIFICATION_ID = 12345;

    private static final long RINGTONE_DURATION = 60 * 1000; // 60 giây

    private Handler handler;
    private Runnable stopRingtoneRunnable;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "========== SERVICE CREATED ==========");
        createNotificationChannel();
        handler = new Handler(Looper.getMainLooper());
    }

    @SuppressLint("ForegroundServiceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "========== SERVICE STARTED ==========");

        // 1. Lấy dữ liệu an toàn
        int eventId = intent.getIntExtra("EVENT_ID", -1);
        String eventTitle = intent.getStringExtra("EVENT_TITLE");
        long startTimeMillis = intent.getLongExtra("EVENT_START_TIME", 0);

        // Fix lỗi SystemUI Crash do title null
        if (eventTitle == null || eventTitle.isEmpty()) {
            eventTitle = "Event";
        }

        // 2. Tạo Notification hoàn chỉnh (Chứa nút Hủy + Thông tin)
        Notification notification = buildEventNotification(eventId, eventTitle, startTimeMillis);

        // 3. Kích hoạt Foreground Service bằng chính Notification đó
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE);
            } else {
                startForeground(NOTIFICATION_ID, notification);
            }
            Log.e(TAG, "Foreground Service started with Event Notification");
        } catch (Exception e) {
            Log.e(TAG, "Error starting foreground: " + e.getMessage());
            // Fallback
            startForeground(NOTIFICATION_ID, notification);
        }

        // 4. Phát nhạc chuông
        RingtonePlayer.play(this);
        Log.e(TAG, "Ringtone started");

        // 5. Hẹn giờ tự tắt sau 1 phút
        stopRingtoneRunnable = () -> {
            Log.e(TAG, "Auto-stop ringtone after 1 minute");
            stopServiceAndHideNotification();
        };
        handler.postDelayed(stopRingtoneRunnable, RINGTONE_DURATION);

        return START_NOT_STICKY;
    }

    // Hàm xây dựng thông báo chi tiết
    private Notification buildEventNotification(int eventId, String eventTitle, long startTimeMillis) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        String startTimeFormatted = sdf.format(new Date(startTimeMillis));

        // Intent mở App khi bấm vào thông báo ở chế độ View Mode
        Intent openAppIntent = new Intent(this, AddEventActivity.class);
        // Cờ này giúp tạo Task mới, tránh xung đột nếu App đang mở
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        // Truyền các tham số cần thiết
        openAppIntent.putExtra("is_view_mode", true); // Kích hoạt chế độ xem
        openAppIntent.putExtra("target_event_id", eventId); // Gửi ID để Activity tự load

        PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                this, eventId, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        // Intent cho nút "TẮT" (Gửi tới StopRingtoneReceiver)
        Intent stopIntent = new Intent(this, StopRingtoneReceiver.class);
        stopIntent.setAction(StopRingtoneReceiver.ACTION_STOP_RINGTONE);

        // RequestCode khác nhau để không bị ghi đè nếu có nhiều pending intent
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(
                this, eventId + 1000, stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_clock) // Đảm bảo icon này có trong res/drawable
                .setContentTitle("🔔 " + eventTitle)
                .setContentText("Start at: " + startTimeFormatted)
                // BigTextStyle giúp hiển thị text dài mà không gây lỗi layout
                .setStyle(new NotificationCompat.BigTextStyle().bigText("The event took place at " + startTimeFormatted + ". Chạm để mở."))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)

                // QUAN TRỌNG: Giữ thông báo luôn hiện diện
                .setOngoing(true)
                .setAutoCancel(false)

                .setFullScreenIntent(openAppPendingIntent, true) // Hiện Popup
                .setContentIntent(openAppPendingIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Turn off", stopPendingIntent); // Nút Tắt

        return builder.build();
    }

    // Hàm dừng service và ẩn thông báo
    private void stopServiceAndHideNotification() {
        // 1. Dừng nhạc
        RingtonePlayer.stop();

        // 2. Hủy hẹn giờ
        if (handler != null && stopRingtoneRunnable != null) {
            handler.removeCallbacks(stopRingtoneRunnable);
        }

        // 3. Dừng Foreground -> Thông báo sẽ biến mất
        stopForeground(true);
        stopSelf();

        Log.e(TAG, "Service stopped & Notification removed");
    }

    @Override
    public void onDestroy() {
        // Đảm bảo dọn dẹp sạch sẽ nếu service bị kill bất ngờ
        RingtonePlayer.stop();
        super.onDestroy();
        Log.e(TAG, "========== SERVICE DESTROYED ==========");
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager == null) return;

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Nhắc nhở sự kiện",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Thông báo báo thức sự kiện");
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setSound(null, null);

            manager.createNotificationChannel(channel);
            Log.e(TAG, "Channel created");
        }
    }
}