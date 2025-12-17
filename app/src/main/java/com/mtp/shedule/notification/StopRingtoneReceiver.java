package com.mtp.shedule.notification;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class StopRingtoneReceiver extends BroadcastReceiver {
    private static final String TAG = "StopRingtoneReceiver";
    public static final String ACTION_STOP_RINGTONE = "com.mtp.shedule.STOP_RINGTONE";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.e(TAG, "========== STOP RINGTONE TRIGGERED ==========");

        if (ACTION_STOP_RINGTONE.equals(intent.getAction())) {
            // 1. Dừng nhạc ngay để phản hồi người dùng
            RingtonePlayer.stop();

            // 2. Dừng Service - sẽ kích hoạt onDestroy() của Service
            Intent serviceIntent = new Intent(context, ReminderForegroundService.class);
            context.stopService(serviceIntent);

            // 3. Xóa notification sự kiện
            int notificationId = intent.getIntExtra("NOTIFICATION_ID", -1);
            if (notificationId != -1) {
                NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                nm.cancel(notificationId);
            }
        }

        Log.e(TAG, "========== STOP RINGTONE FINISHED ==========");
    }
}