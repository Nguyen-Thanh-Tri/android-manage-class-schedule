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
            Log.e(TAG, "AlarmManager is null. Cannot schedule.");
            return;
        }

        // THÊM: Kiểm tra quyền SCHEDULE_EXACT_ALARM (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e(TAG, "Không có quyền SCHEDULE_EXACT_ALARM!");
                // Yêu cầu người dùng cấp quyền
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                return;
            }
        }

        // 1. Kiểm tra xem có cần nhắc nhở không
        if (event.getReminder() <= 0) {
            cancelReminder(context, event.getId());
            return;
        }

        // 2. Tính toán thời điểm thông báo
        long reminderMinutes = (long) event.getReminder();
        long reminderMillis = reminderMinutes * 60 * 1000;
        long triggerAtMillis = event.getStartTime() - reminderMillis;
        long currentTime = System.currentTimeMillis();

        // Log để kiểm tra độ lệch thời gian
        Log.d(TAG, "Planning schedule for EventID: " + event.getId() +
                " | TriggerTime: " + triggerAtMillis +
                " | CurrentTime: " + currentTime +
                " | Diff: " + (triggerAtMillis - currentTime));

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

        // 4. Đặt lịch với setAlarmClock (Độ ưu tiên cao nhất)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Tạo PendingIntent thứ 2 để khi người dùng nhấn vào icon báo thức trên
            // màn hình khóa/thanh trạng thái, nó sẽ mở ứng dụng (AddEventActivity hoặc MainActivity)
            PendingIntent showIntent = getShowAppPendingIntent(context, event.getId());

            AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(
                    triggerAtMillis,
                    showIntent
            );

            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
            Log.i(TAG, "Scheduled using setAlarmClock (High Priority) for ID: " + event.getId());
        }
//        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            // Fallback cho Android cũ (dưới 5.0)
//            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
//        }
        else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        }
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
                pendingIntent.cancel();
                Log.i(TAG, "Cancelled alarm for ID: " + eventId);
            }
        }
    }

    // Hàm phụ trợ tạo Intent mở ứng dụng khi nhấn vào icon báo thức hệ thống
    private static PendingIntent getShowAppPendingIntent(Context context, int eventId) {
        // Bạn có thể đổi AddEventActivity.class thành MainActivity.class nếu muốn
        Intent intent = new Intent(context, AddEventActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        // Dùng request code khác (eventId + 10000) để tránh trùng với PendingIntent của Broadcast
        return PendingIntent.getActivity(context, eventId + 10000, intent, flags);
    }
}