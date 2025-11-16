package com.mtp.shedule;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import com.mtp.shedule.entity.EventEntity;

// Đảm bảo tạo ReminderReceiver trước khi sử dụng
public class NotificationScheduler {
    private static final String TAG = "NotifScheduler";
    public static void scheduleReminder(Context context, EventEntity event) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null. Cannot schedule.");
            return;
        }

        // Kiểm tra xem có cần nhắc nhở không
        if (event.getReminder() <= 0) {
            cancelReminder(context, event.getId()); // Hủy nếu reminder là 0 (None)
            return;
        }

        // Tính toán thời điểm thông báo
        long reminderMinutes = (long) event.getReminder();
        long reminderMillis = reminderMinutes * 60 * 1000;
        long triggerAtMillis = event.getStartTime() - reminderMillis;
        long currentTime = System.currentTimeMillis();

        // Đặt khoảng đệm (buffer) 5 giây (5000ms) để xử lý việc đặt lịch quá sát giờ
        final long SAFETY_BUFFER_MS = 5000;

        // Xử lý 1: Nếu thời điểm đã trôi qua, KHÔNG đặt lịch
        if (triggerAtMillis <= currentTime) {
            Log.w(TAG, "Trigger time (" + triggerAtMillis + ") is in the past or current time. Cancelling.");
            // Bạn có thể cân nhắc gọi cancelReminder ở đây để hủy bỏ nếu có lịch cũ.
            return;
        }

        // Xử lý 2: Kiểm tra Quyền Báo thức Chính xác (MANDATORY cho độ chính xác cao)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "Exact Alarm permission denied. Cannot schedule with high precision.");
                // Thay vì chỉ Toast, bạn cần hướng dẫn người dùng cấp quyền.
                // Tuy nhiên, việc chuyển hướng Intent.FLAG_ACTIVITY_NEW_TASK từ non-Activity context
                // có thể gây ra cảnh báo/lỗi, nên tốt nhất là kiểm tra trong Activity.
                // Ở đây, ta chỉ thông báo cho người dùng và dừng lại.
                Toast.makeText(context, "Vui lòng cấp quyền 'Báo thức chính xác' cho ứng dụng!", Toast.LENGTH_LONG).show();
                return;
            }
        }


        // 2. Chuẩn bị Intent và PendingIntent (Không đổi)
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("EVENT_ID", event.getId());
        intent.putExtra("EVENT_TITLE", event.getTitle());
        intent.putExtra("EVENT_START_TIME", event.getStartTime());

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                event.getId(),
                intent,
                flags
        );


        // 3. Đặt lịch với setExactAndAllowWhileIdle

        // Dùng setExactAndAllowWhileIdle() cho M+ (độ chính xác cao nhất, vượt qua Doze)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        } else {
            // setExact() cho các phiên bản cũ hơn
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
            );
        }

        Log.i(TAG, String.format("Scheduled ID %d for %s. Diff: %d ms",
                event.getId(),
                new java.util.Date(triggerAtMillis).toString(),
                (triggerAtMillis - currentTime)));
    }

    public static void cancelReminder(Context context, int eventId) {
        Intent intent = new Intent(context, ReminderReceiver.class);

        int flags = PendingIntent.FLAG_NO_CREATE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                eventId,
                intent,
                flags
        );

        if (pendingIntent != null) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel(); // Hủy luôn PendingIntent để giải phóng tài nguyên
            }
        }
    }
}