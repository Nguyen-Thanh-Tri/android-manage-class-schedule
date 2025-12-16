package com.mtp.shedule;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.EventEntity;

import java.util.List;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        // Kiểm tra xem có phải tín hiệu khởi động máy không
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.i("BootReceiver", "Thiết bị vừa khởi động lại. Bắt đầu đặt lại báo thức...");

            // Bắt buộc chạy Database trong luồng phụ (Thread) để không làm đơ máy
            new Thread(() -> {
                try {
                    // 1. Lấy kết nối Database
                    ConnDatabase db = ConnDatabase.getInstance(context);

                    // 2. Lấy TOÀN BỘ sự kiện (Sử dụng hàm vừa thêm ở Bước 1)
                    List<EventEntity> allEvents = db.eventDao().getAllEvents();

                    if (allEvents != null && !allEvents.isEmpty()) {
                        for (EventEntity event : allEvents) {
                            // 3. Gọi Scheduler để đặt lại báo thức
                            // Hàm này đã có logic: Nếu giờ đã qua -> Bỏ qua. Nếu chưa -> Đặt lịch.
                            NotificationScheduler.scheduleReminder(context, event);
                        }
                        Log.i("BootReceiver", "Đã khôi phục lịch nhắc nhở cho " + allEvents.size() + " sự kiện.");
                    } else {
                        Log.i("BootReceiver", "Không có sự kiện nào trong Database để khôi phục.");
                    }
                } catch (Exception e) {
                    Log.e("BootReceiver", "Lỗi khi khôi phục báo thức: " + e.getMessage());
                }
            }).start();
        }
    }
}