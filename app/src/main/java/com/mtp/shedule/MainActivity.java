package com.mtp.shedule;

import android.os.Bundle;
import android.view.ViewTreeObserver;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;
import com.mtp.shedule.fragment.CalendarFragment;
import com.mtp.shedule.fragment.ExamsFragment;
import com.mtp.shedule.fragment.SettingsFragment;
import com.mtp.shedule.fragment.TeachersFragment;
import com.mtp.shedule.fragment.TimeTableFragment;

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

        // ---------------- Toolbar ----------------
        toolbar = findViewById(R.id.toolbar);
        toolbarTitle = findViewById(R.id.toolbar_title);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);


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

            if (id == R.id.nav_timetable && !(currentFragment instanceof TimeTableFragment)) {
                newFragment = new TimeTableFragment();
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

    // ---------------- Helper method thay fragment ----------------
    private void replaceFragment(Fragment fragment) {
        currentFragment = fragment;
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

}
