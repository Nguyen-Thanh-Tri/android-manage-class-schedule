package com.mtp.shedule.notification;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.EventEntity;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.i(TAG, "========== BOOT RECEIVER TRIGGERED ==========");
        Log.i(TAG, "Action: " + action);

        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            Log.i(TAG, "Thiết bị vừa khởi động. Đang khôi phục alarms...");

            new Thread(() -> {
                try {
                    ConnDatabase db = ConnDatabase.getInstance(context);
                    List<EventEntity> allEvents = db.eventDao().getAllEvents();

                    if (allEvents != null && !allEvents.isEmpty()) {
                        int scheduled = 0;
                        for (EventEntity event : allEvents) {
                            // Chỉ lên lịch cho event chưa qua
                            long currentTime = System.currentTimeMillis();
                            if (event.getStartTime() > currentTime) {
                                NotificationScheduler.scheduleReminder(context, event);
                                scheduled++;
                            }
                        }
                        Log.i(TAG, "Đã khôi phục " + scheduled + "/" + allEvents.size() + " alarms");
                    } else {
                        Log.i(TAG, "Không có event nào để khôi phục");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Lỗi khôi phục: " + e.getMessage());
                    e.printStackTrace();
                }
            }).start();
        }
    }
}