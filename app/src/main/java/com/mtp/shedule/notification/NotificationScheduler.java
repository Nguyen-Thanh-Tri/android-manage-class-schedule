package com.mtp.shedule.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import com.mtp.shedule.AddEventActivity;
import com.mtp.shedule.entity.EventEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NotificationScheduler {
    private static final String TAG = "NotifScheduler";

//    public static void scheduleReminder(Context context, EventEntity event) {
//        Log.e(TAG, ">>> START scheduleReminder for Event ID: " + event.getId());
//        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        if (alarmManager == null) {
//            Log.e(TAG, "AlarmManager is null");
//            return;
//        }
//
//        // 1.Kiểm tra quyền SCHEDULE_EXACT_ALARM (Android 12+)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//            if (!alarmManager.canScheduleExactAlarms()) {
//                Log.e(TAG, "Missing Permission: SCHEDULE_EXACT_ALARM!");
//                return;
//            }
//        }
//
//        // 2.Kiểm tra reminder
//        if (event.getReminder() < 0) {
//            Log.d(TAG, "Event has no reminder set. Skipping.");
//            cancelReminder(context, event.getId());
//            return;
//        }
//
//        // 3.Tính toán thời gian (logic 24h/00:00)
//        long reminderMillis = (long) event.getReminder() * 60 * 1000;
//        long eventStartTime = event.getStartTime();
//        long triggerAtMillis = eventStartTime - reminderMillis;
//        long currentTime = System.currentTimeMillis();
//
//        // --- DEBUG THỜI GIAN (Quan trọng để fix lỗi 00:00) ---
//        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
//        String eventTimeStr = sdf.format(new Date(eventStartTime));
//        String triggerTimeStr = sdf.format(new Date(triggerAtMillis));
//        String currentStr = sdf.format(new Date(currentTime));
//
//        Log.d(TAG, " Event Time:   " + eventTimeStr + " (Raw: " + eventStartTime + ")");
//        Log.d(TAG, " Trigger Time: " + triggerTimeStr + " (Raw: " + triggerAtMillis + ")");
//        Log.d(TAG, " Current Time: " + currentStr);
//        Log.d(TAG, " Reminder: " + event.getReminder() + " minutes before");
//
//        // 4.Nếu thời điểm kích hoạt đã trôi qua, không đặt lịch
//        if (triggerAtMillis <= currentTime) {
//            Log.w(TAG, "Trigger time is in the past. Ignoring.");
//            return;
//        }
//
//        // 5.Tạo Intent
//        Intent intent = new Intent(context, ReminderReceiver.class);
//        intent.putExtra("EVENT_ID", event.getId());
//        intent.putExtra("EVENT_TITLE", event.getTitle());
//        intent.putExtra("EVENT_START_TIME", event.getStartTime());
//        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
//
//        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            flags |= PendingIntent.FLAG_IMMUTABLE;
//        }
//
//        PendingIntent pendingIntent = PendingIntent.getBroadcast(
//                context,
//                event.getId(),
//                intent,
//                flags
//        );
//
//        // 6.Đặt lịch
//        try {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                alarmManager.setExactAndAllowWhileIdle(
//                        AlarmManager.RTC_WAKEUP,
//                        triggerAtMillis,
//                        pendingIntent
//                );
//                Log.i(TAG, "Scheduled using setExactAndAllowWhileIdle for ID: " + event.getId());
//            } else {
//                alarmManager.setExact(
//                        AlarmManager.RTC_WAKEUP,
//                        triggerAtMillis,
//                        pendingIntent
//                );
//                Log.i(TAG, "SUCCESS: Alarm scheduled for" + event.getId());
//            }
//        } catch (SecurityException e) {
//            Log.e(TAG, "SecurityException: " + e.getMessage());
//        }
//    }
public static void scheduleReminder(Context context, EventEntity event) {
    Log.e(TAG, ">>> START scheduleReminder for Event ID: " + event.getId());
    AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    if (alarmManager == null) {
        Log.e(TAG, "AlarmManager is null");
        return;
    }

    // 1. Kiểm tra quyền SCHEDULE_EXACT_ALARM (Android 12+)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (!alarmManager.canScheduleExactAlarms()) {
            Log.e(TAG, "Missing Permission: SCHEDULE_EXACT_ALARM!");
            // Có thể mở intent để request permission
            Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            return;
        }
    }

    // 2. Kiểm tra reminder
    if (event.getReminder() < 0) {
        Log.d(TAG, "Event has no reminder set. Skipping.");
        cancelReminder(context, event.getId());
        return;
    }

    // 3. Tính toán thời gian (logic 24h/00:00)
    long reminderMillis = (long) event.getReminder() * 60 * 1000;
    long eventStartTime = event.getStartTime();
    long triggerAtMillis = eventStartTime - reminderMillis;
    long currentTime = System.currentTimeMillis();

    // 4. Debug thời gian
    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault());
    String eventTimeStr = sdf.format(new Date(eventStartTime));
    String triggerTimeStr = sdf.format(new Date(triggerAtMillis));
    String currentStr = sdf.format(new Date(currentTime));

    Log.d(TAG, " Event Time:   " + eventTimeStr + " (Raw: " + eventStartTime + ")");
    Log.d(TAG, " Trigger Time: " + triggerTimeStr + " (Raw: " + triggerAtMillis + ")");
    Log.d(TAG, " Current Time: " + currentStr);
    Log.d(TAG, " Reminder: " + event.getReminder() + " minutes before");

    // 5. Nếu thời điểm kích hoạt đã trôi qua, không đặt lịch
    if (triggerAtMillis <= currentTime) {
        Log.w(TAG, "Trigger time is in the past. Ignoring.");
        return;
    }

    // 6. PHÂN LOẠI: Dùng AlarmClock cho reminder quan trọng
    if (event.getReminder() == 0) {
        // Event bắt đầu - ƯU TIÊN CAO: sử dụng Alarm Clock
        scheduleAsAlarmClock(context, event, triggerAtMillis);
    } else {
        // Reminder trước đó - Sử dụng phương thức chính xác
        scheduleWithExactAlarm(context, event, triggerAtMillis);
    }
}

    // THÊM PHƯƠNG THỨC MỚI (sau phương thức scheduleReminder):
    private static void scheduleAsAlarmClock(Context context, EventEntity event, long triggerAtMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        // Intent hiển thị trên đồng hồ (khi user bấm vào thông báo đồng hồ)
        Intent showIntent = new Intent(context, AddEventActivity.class);
        showIntent.putExtra("target_event_id", event.getId());
        showIntent.putExtra("is_view_mode", true);
        showIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent showPendingIntent = PendingIntent.getActivity(
                context,
                event.getId() + 10000, // Different request code
                showIntent,
                flags
        );

        // Intent cho BroadcastReceiver khi alarm kích hoạt
        Intent alarmIntent = new Intent(context, ReminderReceiver.class);
        alarmIntent.putExtra("EVENT_ID", event.getId());
        alarmIntent.putExtra("EVENT_TITLE", event.getTitle());
        alarmIntent.putExtra("EVENT_START_TIME", event.getStartTime());
        alarmIntent.putExtra("EVENT_REMINDER", event.getReminder());

        PendingIntent alarmPendingIntent = PendingIntent.getBroadcast(
                context,
                event.getId(),
                alarmIntent,
                flags
        );

        // Tạo AlarmClockInfo - HIỂN THỊ TRÊN ĐỒNG HỒ
        AlarmManager.AlarmClockInfo alarmClockInfo =
                new AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent);

        try {
            alarmManager.setAlarmClock(alarmClockInfo, alarmPendingIntent);
            Log.i(TAG, "✅ ALARM CLOCK set for Event ID: " + event.getId() +
                    " at " + new SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                    .format(new Date(triggerAtMillis)));
        } catch (SecurityException e) {
            Log.e(TAG, "❌ Cannot set alarm clock: " + e.getMessage());
            // Fallback to exact alarm
            scheduleWithExactAlarm(context, event, triggerAtMillis);
        }
    }

    private static void scheduleWithExactAlarm(Context context, EventEntity event, long triggerAtMillis) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("EVENT_ID", event.getId());
        intent.putExtra("EVENT_TITLE", event.getTitle());
        intent.putExtra("EVENT_START_TIME", event.getStartTime());
        intent.putExtra("EVENT_REMINDER", event.getReminder());

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

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                );
            }
            Log.i(TAG, "✅ Exact alarm scheduled for Event ID: " + event.getId());
        } catch (SecurityException e) {
            Log.e(TAG, "❌ Cannot set exact alarm: " + e.getMessage());
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