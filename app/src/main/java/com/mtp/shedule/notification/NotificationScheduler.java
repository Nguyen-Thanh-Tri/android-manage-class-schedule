package com.mtp.shedule.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.mtp.shedule.AddEventActivity;
import com.mtp.shedule.entity.EventEntity;

public class NotificationScheduler {
    private static final String TAG = "NotifScheduler";

    public static void scheduleReminder(Context context, EventEntity event) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null");
            return;
        }

        // Kiểm tra quyền SCHEDULE_EXACT_ALARM (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "Không có quyền SCHEDULE_EXACT_ALARM!");
                return;
            }
        }

        // Kiểm tra reminder
        if (event.getReminder() <= 0) {
            cancelReminder(context, event.getId());
            return;
        }

        // Tính toán thời gian
        long reminderMinutes = (long) event.getReminder();
        long reminderMillis = reminderMinutes * 60 * 1000;
        long triggerAtMillis = event.getStartTime() - reminderMillis;
        long currentTime = System.currentTimeMillis();

        // Log để kiểm tra độ lệch thời gian
        Log.d(TAG, "EventID: " + event.getId() +
                " | Trigger: " + triggerAtMillis +
                " | Current: " + currentTime +
                " | Diff: " + (triggerAtMillis - currentTime) + "ms");

        // Nếu thời điểm kích hoạt đã trôi qua, không đặt lịch
        if (triggerAtMillis <= currentTime) {
            Log.w(TAG, "Trigger time is in the past. Ignoring.");
            return;
        }

        // 3. Chuẩn bị PendingIntent gửi tới Receiver (để hiện thông báo)
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("EVENT_ID", event.getId());
        intent.putExtra("EVENT_TITLE", event.getTitle());
        intent.putExtra("EVENT_START_TIME", event.getStartTime());
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);

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

        // 4. Đặt lịch với  setExactAndAllowWhileIdle() THAY VÌ setAlarmClock()
        // Lý do: setAlarmClock() cần quyền cao hơn và có thể bị block trên một số thiết bị
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                );
                Log.i(TAG, "Scheduled using setExactAndAllowWhileIdle for ID: " + event.getId());
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                );
                Log.i(TAG, "Scheduled using setExact for ID: " + event.getId());
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException: " + e.getMessage());
        }
    }

    public static void cancelReminder(Context context, int eventId) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        int flags = PendingIntent.FLAG_NO_CREATE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
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
                pendingIntent.cancel();
                Log.i(TAG, "Cancelled alarm for ID: " + eventId);
            }
        }
    }
}