package com.mtp.shedule.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.mtp.shedule.entity.EventEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationScheduler {
    private static final String TAG = "NotifScheduler";

    public static void scheduleReminder(Context context, EventEntity event) {
        Log.e(TAG, ">>> START scheduleReminder for Event ID: " + event.getId());
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager is null");
            return;
        }

        // 1.Kiểm tra quyền SCHEDULE_EXACT_ALARM (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "Missing Permission: SCHEDULE_EXACT_ALARM!");
                return;
            }
        }

        // 2.Kiểm tra reminder
        if (event.getReminder() < 0) {
            Log.d(TAG, "Event has no reminder set. Skipping.");
            cancelReminder(context, event.getId());
            return;
        }

        // 3.Tính toán thời gian (logic 24h/00:00)
        long reminderMillis = (long) event.getReminder() * 60 * 1000;
        long eventStartTime = event.getStartTime();
        long triggerAtMillis = eventStartTime - reminderMillis;
        long currentTime = System.currentTimeMillis();

        // --- DEBUG THỜI GIAN (Quan trọng để fix lỗi 00:00) ---
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
        String eventTimeStr = sdf.format(new Date(eventStartTime));
        String triggerTimeStr = sdf.format(new Date(triggerAtMillis));
        String currentStr = sdf.format(new Date(currentTime));

        Log.d(TAG, " Event Time:   " + eventTimeStr + " (Raw: " + eventStartTime + ")");
        Log.d(TAG, " Trigger Time: " + triggerTimeStr + " (Raw: " + triggerAtMillis + ")");
        Log.d(TAG, " Current Time: " + currentStr);
        Log.d(TAG, " Reminder: " + event.getReminder() + " minutes before");

        // 4.Nếu thời điểm kích hoạt đã trôi qua, không đặt lịch
        if (triggerAtMillis <= currentTime) {
            Log.w(TAG, "Trigger time is in the past. Ignoring.");
            return;
        }

        // 5.Tạo Intent
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

        // 6.Đặt lịch
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
                Log.i(TAG, "SUCCESS: Alarm scheduled for" + event.getId());
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