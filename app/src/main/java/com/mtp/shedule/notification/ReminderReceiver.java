package com.mtp.shedule.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class ReminderReceiver extends BroadcastReceiver {
    private static final String TAG = "ReminderReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "========== RECEIVER TRIGGERED ==========");
        Log.e(TAG, "Time: " + System.currentTimeMillis());

        // Lấy dữ liệu từ Intent
        int eventId = intent.getIntExtra("EVENT_ID", -1);
        String eventTitle = intent.getStringExtra("EVENT_TITLE");
        long startTimeMillis = intent.getLongExtra("EVENT_START_TIME", 0);

        Log.e(TAG, "EventID: " + eventId);
        Log.e(TAG, "Title: " + eventTitle);
        Log.e(TAG, "StartTime: " + startTimeMillis);

        if (eventId == -1 || eventTitle == null || eventTitle.isEmpty()) {
            Log.e(TAG, "Invalid data");
            return;
        }

        // ✅ KHỞI ĐỘNG FOREGROUND SERVICE
        Intent serviceIntent = new Intent(context, ReminderForegroundService.class);
        serviceIntent.putExtra("EVENT_ID", eventId);
        serviceIntent.putExtra("EVENT_TITLE", eventTitle);
        serviceIntent.putExtra("EVENT_START_TIME", startTimeMillis);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0+ BẮT BUỘC dùng startForegroundService()
                context.startForegroundService(serviceIntent);
                Log.e(TAG, "Foreground Service started (Android 8.0+)");
            } else {
                // Android cũ hơn dùng startService() bình thường
                context.startService(serviceIntent);
                Log.e(TAG, "Service started (Android < 8.0)");
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to start service: " + e.getMessage());
            e.printStackTrace();
        }

        Log.e(TAG, "========== RECEIVER FINISHED ==========");
    }
}