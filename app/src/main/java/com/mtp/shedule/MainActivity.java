package com.mtp.shedule;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.mtp.shedule.fragment.ExamsFragment;
import com.mtp.shedule.fragment.SettingsFragment;
import com.mtp.shedule.fragment.TeachersFragment;
import com.mtp.shedule.fragment.TimeTableFragment;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private Toolbar toolbar;
    private FloatingActionButton fabAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ---------------- Toolbar ----------------
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // ---------------- Drawer Layout ----------------
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

        // Load fragment mặc định (TimeTableFragment)
        if (savedInstanceState == null) {
            replaceFragment(new TimeTableFragment());
            navigationView.setCheckedItem(R.id.nav_timetable);
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment selectedFragment = null;
            String title = Objects.requireNonNull(item.getTitle()).toString(); // lấy title từ menu item

            if (id == R.id.nav_timetable) {
                selectedFragment = new TimeTableFragment();
            } else if (id == R.id.nav_exams) {
                selectedFragment = new ExamsFragment();
            } else if (id == R.id.nav_teachers) {
                selectedFragment = new TeachersFragment();
            } else if (id == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                replaceFragment(selectedFragment);
                navigationView.setCheckedItem(id);

                TextView toolbarTitle = findViewById(R.id.toolbar_title);
                toolbarTitle.setText(title);
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });
    }

    // ---------------- Helper method thay fragment ----------------
    private void replaceFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
