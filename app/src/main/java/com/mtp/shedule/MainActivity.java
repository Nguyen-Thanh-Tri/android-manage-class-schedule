package com.mtp.shedule;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
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
    List<Course> courseList = new ArrayList<>();

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

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerViewCourses);
        fabAdd = findViewById(R.id.fabAdd);

        dbHelper = new DatabaseHelper(this);
        courseList = dbHelper.getAllCourses(); // Lấy dữ liệu từ SQLite

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        courseAdapter = new CourseAdapter(this, courseList);
        recyclerView.setAdapter(courseAdapter);

        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddCourseActivity.class);
            startActivity(intent);
        });
    }

    private void loadCoursesFromDatabase() {
        // Ví dụ dữ liệu tĩnh
        courseList.add(new Course(1, "Android Development", "Ritesh Deshmukh", "704", "08:00", "10:00"));
        courseList.add(new Course(2, "Design Thinking", "Kriti Sanon", "203", "13:30", "15:00"));
        courseList.add(new Course(3, "Data Visualization", "Sunny Deol", "316", "16:30", "17:30"));
    }
}
