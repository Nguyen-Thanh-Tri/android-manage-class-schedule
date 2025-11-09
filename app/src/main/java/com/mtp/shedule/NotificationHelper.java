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

    public static void scheduleAllClassNotifications(Context context) {
        DatabaseHelper db = new DatabaseHelper(context);
        List<Course> courses = db.getAllCourses();

        for (Course course : courses) {
            scheduleClassNotification(context, course);
        }
    }

    public static void scheduleClassNotification(Context context, Course course) {
        try {
            String startTime = course.getTimeStart();
            if (startTime == null || startTime.isEmpty()) return;

            // Parse thời gian bắt đầu (format: "08:00")
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            Calendar classTime = Calendar.getInstance();
            classTime.setTime(sdf.parse(startTime));

            // Tính thời điểm thông báo (2 tiếng trước)
            Calendar notificationTime = (Calendar) classTime.clone();
            notificationTime.add(Calendar.HOUR_OF_DAY, -2);
//            notificationTime.add(Calendar.MINUTE, 2)

            // Nếu thời điểm thông báo đã qua trong ngày hôm nay, chuyển sang ngày mai
            Calendar now = Calendar.getInstance();
            if (notificationTime.before(now)) {
                notificationTime.add(Calendar.DAY_OF_YEAR, 1);
            }

            // Tạo intent cho alarm
            Intent notificationIntent = new Intent(context, NotificationReceiver.class);
            notificationIntent.putExtra("course_title", course.getTitle());
            notificationIntent.putExtra("course_teacher", course.getTeacher());
            notificationIntent.putExtra("course_room", course.getRoom());
            notificationIntent.putExtra("course_time", startTime);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    course.getId(), // Sử dụng ID course làm request code
                    notificationIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Lên lịch alarm
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    notificationTime.getTimeInMillis(),
                    pendingIntent
            );

            Log.d("NotificationHelper", "Đã lên lịch thông báo cho: " + course.getTitle() +
                    " lúc " + notificationTime.getTime());

        } catch (ParseException e) {
            e.printStackTrace();
        }
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