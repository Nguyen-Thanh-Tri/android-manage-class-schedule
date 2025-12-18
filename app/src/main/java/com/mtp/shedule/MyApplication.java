package com.mtp.shedule;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;
import com.mtp.shedule.utils.AppSettings; // Đảm bảo đường dẫn này đúng với file AppSettings của bạn

public class MyApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        AppSettings settings = new AppSettings(this);

        // 2. Lấy chế độ Theme đã lưu
        int mode = settings.getThemeMode();

        // 3. Áp dụng Theme
        AppCompatDelegate.setDefaultNightMode(mode);
    }
}