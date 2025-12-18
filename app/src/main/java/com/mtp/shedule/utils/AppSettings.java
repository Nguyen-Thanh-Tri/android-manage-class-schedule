package com.mtp.shedule.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;

import androidx.appcompat.app.AppCompatDelegate;

import com.mtp.shedule.R;

public class AppSettings {
    private static final String PREF_NAME = "app_settings";
    private static final String KEY_THEME = "key_theme_mode";
    private static final String KEY_RINGTONE = "key_ringtone_uri";
    private static final String KEY_RINGTONE_NAME = "key_ringtone_name";

    private final SharedPreferences prefs;
    private final Context context;

    public AppSettings(Context context) {
        this.context = context;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    // --- DARK MODE ---
    public void saveThemeMode(int mode) {
        prefs.edit().putInt(KEY_THEME, mode).apply();
    }

    public int getThemeMode() {
        return prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public void applyTheme() {
        AppCompatDelegate.setDefaultNightMode(getThemeMode());
    }

    // --- RINGTONE ---

    // Lưu đường dẫn (URI) của nhạc chuông
    public void saveRingtone(Uri uri, String name) {
        String uriString = (uri != null) ? uri.toString() : "";
        prefs.edit()
                .putString(KEY_RINGTONE, uriString)
                .putString(KEY_RINGTONE_NAME, name)
                .apply();
    }

    // Lấy đường dẫn nhạc chuông (để phát khi thông báo)
    public Uri getRingtoneUri() {
        String uriString = prefs.getString(KEY_RINGTONE, "");

        // Nếu chưa lưu gì cả -> Trả về Cat 1 mặc định
        if (uriString.isEmpty()) {
            return Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.cat1);
        }
        return Uri.parse(uriString);
    }

    public String getRingtoneName() {
        // Tên mặc định hiển thị
        return prefs.getString(KEY_RINGTONE_NAME, "Cat 1 (Mặc định)");
    }
}