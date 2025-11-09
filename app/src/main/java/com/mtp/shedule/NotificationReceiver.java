// File mới: NotificationReceiver.java
package com.mtp.shedule;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

public class NotificationReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String courseTitle = intent.getStringExtra("course_title");
        String courseTeacher = intent.getStringExtra("course_teacher");
        String courseRoom = intent.getStringExtra("course_room");
        String courseTime = intent.getStringExtra("course_time");

        showNotification(context, courseTitle, courseTeacher, courseRoom, courseTime);
    }

    private void showNotification(Context context, String title, String teacher, String room, String time) {
        String notificationText = "Giảng viên: " + teacher +
                (room != null && !room.isEmpty() ? " - Phòng: " + room : "") +
                " - Bắt đầu: " + time;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "CLASS_SCHEDULE_CHANNEL")
                .setSmallIcon(R.drawable.ic_time) // Sử dụng icon có sẵn
                .setContentTitle("Sắp có tiết học: " + title)
                .setContentText(notificationText)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationText))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }
}