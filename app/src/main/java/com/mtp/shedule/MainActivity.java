package com.mtp.shedule;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    DatabaseHelper dbHelper;
    FloatingActionButton fabAdd;
    CourseAdapter courseAdapter;
    CourseAdapter adapter;
    DrawerLayout drawerLayout;
    Toolbar toolbar;
    List<Course> courseList = new ArrayList<>();

    private LinearLayout menuTimeTable, menuExams, menuTeachers, menuSettings;

    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        recyclerView = findViewById(R.id.recyclerViewCourses);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//
//        loadCoursesFromDatabase(); // load từ SQLite hoặc Room
//
//        adapter = new CourseAdapter(this, courseList);
//        recyclerView.setAdapter(adapter);
//    }
    //    private void loadCoursesFromDatabase() {
//        // Ví dụ dữ liệu tĩnh
//        courseList.add(new Course(1, "Android Development", "Ritesh Deshmukh", "704", "08:00", "10:00"));
//        courseList.add(new Course(2, "Design Thinking", "Kriti Sanon", "203", "13:30", "15:00"));
//        courseList.add(new Course(3, "Data Visualization", "Sunny Deol", "316", "16:30", "17:30"));
//    }

    //Thêm 1 card
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ---------------- Toolbar & Drawer ----------------
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

        // ---------------- Menu items click ----------------
        menuTimeTable = findViewById(R.id.menu_timetable);
        menuExams = findViewById(R.id.menu_exams);
        menuTeachers = findViewById(R.id.menu_teachers);
        menuSettings = findViewById(R.id.menu_settings);

        menuTimeTable.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        menuExams.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            // TODO: chuyển sang màn Exams nếu có
        });

        menuTeachers.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            // TODO: mở danh sách giảng viên
        });

        menuSettings.setOnClickListener(v -> {
            drawerLayout.closeDrawer(GravityCompat.START);
            //
        });
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
