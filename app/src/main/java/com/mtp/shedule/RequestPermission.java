package com.mtp.shedule;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class RequestPermission{
    public static final int PERMISSION_REQUEST_CODE = 100;
    public static void requestNotificationPermission(Activity activity) {
        // Chỉ cần kiểm tra và yêu cầu nếu đang chạy Android 13 (API 33) trở lên
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED) {

                // Yêu cầu quyền từ người dùng
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_CODE
                );
            }
        }
    }
}