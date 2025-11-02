package com.mtp.shedule;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    DatabaseHelper dbHelper;
    FloatingActionButton fabAdd;
    CourseAdapter courseAdapter;
    DrawerLayout drawerLayout;
    Toolbar toolbar;
    List<Course> courseList = new ArrayList<>();
    Fragment currentFragment;
    LinearLayout llDays;
    MaterialButton[] dayButtons;



    //Thêm 1 card
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ---------------- RecyclerView setup ----------------
        recyclerView = findViewById(R.id.recyclerViewCourses);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        dbHelper = new DatabaseHelper(this);
        courseList = dbHelper.getAllCourses(); // Lấy dữ liệu từ SQLite

        courseAdapter = new CourseAdapter(this, courseList);
        recyclerView.setAdapter(courseAdapter);

        // ---------------- Floating Action Button ----------------
        fabAdd = findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddCourseActivity.class);
            startActivity(intent);
        });

        // ---------------- Toolbar & Drawer ----------------
        // ẩn và hiện menu
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);

        toolbar.setNavigationOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        // ---------------- Navigation Drawer ----------------
        //chuyển đổi giữa các fragment
        NavigationView navigationView = findViewById(R.id.navigation_view);

        if (savedInstanceState == null) {
            currentFragment = new TimeTableFragment();
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, currentFragment)
                    .commit();
            navigationView.setCheckedItem(R.id.nav_timetable);
        }

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_timetable) {
                // load fragment TimeTable
            } else if (id == R.id.nav_exams) {
                // load fragment Exams
            } else if (id == R.id.nav_teachers) {
                // load fragment Teachers
            } else if (id == R.id.nav_settings) {
                // load fragment Settings
            }

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });


        llDays = findViewById(R.id.ll_days);
        dayButtons = new MaterialButton[]{
                findViewById(R.id.btnMonday),
                findViewById(R.id.btnTuesday),
                findViewById(R.id.btnWednesday),
                findViewById(R.id.btnThursday),
                findViewById(R.id.btnFriday),
                findViewById(R.id.btnSaturday),
                findViewById(R.id.btnSunday)
        };

        // --- Xác định thứ hôm nay ---
        int today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        // Calendar.SUNDAY = 1, MONDAY = 2...
        int index = (today == Calendar.SUNDAY) ? 6 : today - 2; // 0-based index: Mon=0 ... Sun=6

        highlightDay(index);
        loadCoursesByDay(dayButtons[index].getText().toString());

        // --- set click listener cho từng button ---
        for (int i = 0; i < dayButtons.length; i++) {
            int finalI = i;
            dayButtons[i].setOnClickListener(v -> {
                highlightDay(finalI);
                String day = dayButtons[finalI].getText().toString();
                loadCoursesByDay(day);
            });
        }
    }



    private void highlightDay(int index){
        for(int i = 0; i < dayButtons.length; i++){
            if(i == index){
                dayButtons[i].setBackgroundColor(getResources().getColor(R.color.orange)); // hoặc #FFA500
                dayButtons[i].setTextColor(getResources().getColor(android.R.color.white));
            } else {
                dayButtons[i].setBackgroundColor(getResources().getColor(android.R.color.transparent));
                dayButtons[i].setTextColor(getResources().getColor(android.R.color.black));
            }
        }

    }

    // --- Load môn học theo ngày ---
    private void loadCoursesByDay(String day){
        courseList.clear();
        courseList.addAll(dbHelper.getCoursesByDay(day)); // db.getCoursesByDay trả về list theo dayOfWeek
        if(courseAdapter == null){
            courseAdapter = new CourseAdapter(this,courseList);
            recyclerView.setAdapter(courseAdapter);
        } else {
            courseAdapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Reload dữ liệu mỗi khi quay lại MainActivity
        courseList.clear();
        courseList.addAll(dbHelper.getAllCourses());
        courseAdapter.notifyDataSetChanged();
    }

}
