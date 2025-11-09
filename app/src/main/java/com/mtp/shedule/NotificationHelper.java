// File mới: NotificationHelper.java
package com.mtp.shedule;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class NotificationHelper {
    private static final String CHANNEL_ID = "CLASS_SCHEDULE_CHANNEL";
    private static final int NOTIFICATION_ID = 1001;


public static int[] scheduleAllClassNotifications(Context context) {
    DatabaseHelper db = new DatabaseHelper(context);
    List<Course> courses = db.getAllCourses();

    int scheduledCount = 0;
    int skippedCount = 0;

    Log.d("NotificationHelper", "Bắt đầu lên lịch cho " + courses.size() + " môn học");

    for (Course course : courses) {
        Log.d("NotificationHelper", "Xử lý môn: " + course.getTitle() + " - Thời gian: " + course.getTimeStart());

        if (scheduleClassNotification(context, course)) {
            scheduledCount++;
            Log.i("NotificationHelper", "✅ Đã lên lịch: " + course.getTitle());
        } else {
            skippedCount++;
            Log.w("NotificationHelper", "❌ Bỏ qua: " + course.getTitle() + " - Thời gian: " + course.getTimeStart());
        }
    }

    Log.d("NotificationHelper", "Kết quả: " + scheduledCount + " thành công, " + skippedCount + " thất bại");
    return new int[]{scheduledCount, skippedCount};
}
public static boolean scheduleClassNotification(Context context, Course course) {
    try {
        // VALIDATION MẠNH HƠN
        String startTime = course.getTimeStart();
        if (startTime == null || startTime.isEmpty()) {
            Log.w("NotificationHelper", "❌ Bỏ qua - Thời gian rỗng: " + course.getTitle());
            return false;
        }

        // KIỂM TRA ĐỊNH DẠNG THỜI GIAN
        if (!isValidTimeFormat(startTime)) {
            Log.w("NotificationHelper", "❌ Bỏ qua - Định dạng thời gian sai: " + course.getTitle() + " - " + startTime);
            return false;
        }

        // XÓA ALARM CŨ
        cancelClassNotification(context, course.getId());

        // PARSE THỜI GIAN
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        Calendar classTime = Calendar.getInstance();
        classTime.setTime(sdf.parse(startTime));

        // QUAN TRỌNG: Set ngày/tháng/năm hiện tại, chỉ giữ lại giờ-phút từ input
        Calendar now = Calendar.getInstance();
        classTime.set(Calendar.YEAR, now.get(Calendar.YEAR));
        classTime.set(Calendar.MONTH, now.get(Calendar.MONTH));
        classTime.set(Calendar.DAY_OF_MONTH, now.get(Calendar.DAY_OF_MONTH));

        // TÍNH THỜI ĐIỂM THÔNG BÁO (2 tiếng trước)
        Calendar notificationTime = (Calendar) classTime.clone();
        notificationTime.add(Calendar.HOUR_OF_DAY, -2);

        // KIỂM TRA THỜI GIAN ĐÃ QUA
//
        if (notificationTime.before(now)) {
            notificationTime.add(Calendar.DAY_OF_YEAR, 1);
        }

        // TẠO INTENT
        Intent notificationIntent = new Intent(context, NotificationReceiver.class);
        notificationIntent.putExtra("course_title", course.getTitle());
        notificationIntent.putExtra("course_teacher", course.getTeacher());
        notificationIntent.putExtra("course_room", course.getRoom());
        notificationIntent.putExtra("course_time", startTime);

        // SỬA: Dùng hash code của title làm request code (tránh ID không phải số)
        int requestCode = Math.abs(course.getTitle().hashCode());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode, // SỬA: Dùng hash code thay vì course.getId()
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        // LÊN LỊCH ALARM
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        // THÊM KIỂM TRA NULL
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, notificationTime.getTimeInMillis(), pendingIntent);
            Log.i("NotificationHelper", "✅ Lên lịch thành công: " + course.getTitle() + " - Thông báo lúc: " + notificationTime.getTime());
            return true;
        } else {
            Log.e("NotificationHelper", "❌ AlarmManager is null");
            return false;
        }

    } catch (ParseException e) {
        Log.e("NotificationHelper", "❌ Lỗi parse thời gian: " + course.getTitle() + " - " + course.getTimeStart());
        e.printStackTrace();
        return false;
    } catch (Exception e) {
        Log.e("NotificationHelper", "❌ Lỗi không xác định: " + course.getTitle() + " - " + e.getMessage());
        e.printStackTrace();
        return false;
    }
}

    // THÊM METHOD KIỂM TRA ĐỊNH DẠNG THỜI GIAN
    private static boolean isValidTimeFormat(String time) {
        if (time == null) return false;
        // Kiểm tra định dạng HH:mm (ví dụ: 08:00, 13:30)
        return time.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$");
    }

    public static void cancelClassNotification(Context context, int courseId) {
        Intent notificationIntent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                courseId,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Lịch học";
            String description = "Thông báo nhắc lịch học";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}