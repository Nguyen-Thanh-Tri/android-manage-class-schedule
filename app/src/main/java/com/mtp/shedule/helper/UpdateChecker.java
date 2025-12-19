package com.mtp.shedule.helper;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UpdateChecker {


    private static final String URL_VERSION = "https://raw.githubusercontent.com/Nguyen-Thanh-Tri/android-manage-class-schedule/refs/heads/main/version.json";

    public static void checkForUpdate(Activity activity) {
        // 1. Hiện Loading để người dùng biết đang check
        ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setMessage("Checking for updates...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                // 2. Tải file từ Server
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(URL_VERSION).build();
                Response response = client.newCall(request).execute();

                if (response.isSuccessful() && response.body() != null) {
                    String jsonData = response.body().string();
                    JSONObject jsonObject = new JSONObject(jsonData);

                    int newVersionCode = jsonObject.getInt("versionCode");
                    String downloadLink = jsonObject.getString("downloadUrl");
                    String changeLog = jsonObject.optString("changelog", "");

                    // 3. Lấy version hiện tại
                    int currentVersionCode = getAppVersionCode(activity);

                    // 4. So sánh và phản hồi về Main Thread
                    handler.post(() -> {
                        progressDialog.dismiss();
                        if (newVersionCode > currentVersionCode) {
                            // CÓ BẢN MỚI
                            showUpdateDialog(activity, downloadLink, changeLog);
                        } else {
                            // KHÔNG CÓ BẢN MỚI
                            Toast.makeText(activity, "You are using the latest version!", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    handler.post(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(activity, "Check failed. Server error.", Toast.LENGTH_SHORT).show();
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(activity, "Check failed. Please check internet.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private static int getAppVersionCode(Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            return pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    private static void showUpdateDialog(Activity activity, String link, String changeLog) {
        if (activity.isFinishing()) return;

        new AlertDialog.Builder(activity)
                .setTitle("New Version Available ")
                .setMessage("A new version is available for download.\n\n" + changeLog)
                .setPositiveButton("Update Now", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
                    activity.startActivity(intent);
                })
                .setNegativeButton("Later", null)
                .show();
    }
}