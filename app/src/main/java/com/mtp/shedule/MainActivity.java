package com.mtp.shedule;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.ViewTreeObserver;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.mtp.shedule.fragment.CalendarFragment;
import com.mtp.shedule.fragment.ExamsFragment;
import com.mtp.shedule.fragment.SettingsFragment;
import com.mtp.shedule.fragment.TeachersFragment;
import com.mtp.shedule.fragment.CourseFragment;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private TextView toolbarTitle;
    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        requestBatteryOptimizationExemption();

        // ---------------- Toolbar ----------------
        toolbar = findViewById(R.id.toolbar);
        toolbarTitle = findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        RequestPermission.requestNotificationPermission(this);

        // Navigation icon mở/đóng Drawer
        drawerLayout = findViewById(R.id.drawer_layout);
        toolbar.setNavigationOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // ---------------- Navigation Drawer ----------------
        NavigationView navigationView = findViewById(R.id.navigation_view);

        // Load fragment mặc định
        if (savedInstanceState == null) {
            replaceFragment(new CalendarFragment());
            navigationView.setCheckedItem(R.id.nav_calendar);
            toolbarTitle.setText(getString(R.string.main_screen_title));
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            String title = Objects.requireNonNull(item.getTitle()).toString();
            Fragment newFragment = null;

            if (id == R.id.nav_timetable && !(currentFragment instanceof CourseFragment)) {
                newFragment = new CourseFragment();
            } else if (id == R.id.nav_exams && !(currentFragment instanceof ExamsFragment)) {
                newFragment = new ExamsFragment();
            } else if (id == R.id.nav_teachers && !(currentFragment instanceof TeachersFragment)) {
                newFragment = new TeachersFragment();
            } else if (id == R.id.nav_settings && !(currentFragment instanceof SettingsFragment)) {
                newFragment = new SettingsFragment();
            } else if (id == R.id.nav_calendar && !(currentFragment instanceof CalendarFragment)) {
                newFragment = new CalendarFragment();
            }

            if (newFragment != null) {
                replaceFragment(newFragment);
                navigationView.setCheckedItem(id);
                toolbarTitle.setText(title);
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == RequestPermission.PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Người dùng đã cấp quyền POST_NOTIFICATIONS.
                // Có thể tiếp tục logic đặt lịch báo thức.
                Toast.makeText(this, "Đã cấp quyền thông báo.", Toast.LENGTH_SHORT).show();
            } else {
                // Người dùng từ chối quyền POST_NOTIFICATIONS.
                Toast.makeText(this, "Không thể hiện thông báo nhắc nhở nếu không cấp quyền.", Toast.LENGTH_LONG).show();
            }
        }
    }
    // ---------------- Helper method thay fragment ----------------
    private void replaceFragment(Fragment fragment) {
        currentFragment = fragment;
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
    private void requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

            if (pm != null && !pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);

                Toast.makeText(this,
                        "Vui lòng tắt tối ưu pin để nhận thông báo đúng giờ",
                        Toast.LENGTH_LONG).show();
            }
        }
    }


}
