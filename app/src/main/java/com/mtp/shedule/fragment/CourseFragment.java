package com.mtp.shedule.fragment;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.mtp.shedule.AddCourseDialog;
import com.mtp.shedule.adapter.CourseAdapter;
import com.mtp.shedule.R;
import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.EventEntity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class CourseFragment extends Fragment {

    private RecyclerView recyclerView;
    private CourseAdapter courseAdapter;
    private ConnDatabase db;
    private final List<EventEntity> courseList = new ArrayList<>();

    private FloatingActionButton fabAdd;
    private TabLayout tabLayout;
    private LiveData<List<EventEntity>> currentLiveData;
    private final String[] daysOfWeek = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
    public CourseFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timetable, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewCourses);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        courseAdapter = new CourseAdapter(getContext(), courseList);
        recyclerView.setAdapter(courseAdapter);

        db = ConnDatabase.getInstance(getContext());

        tabLayout = view.findViewById(R.id.tabLayout);
        setupTabs();

// ---------------- Floating Action Button -Thêm lịch học ----------------
        fabAdd = view.findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            AddCourseDialog dialog = new AddCourseDialog();
            dialog.show(getParentFragmentManager(), "AddCourseDialog");
        });

        // EDIT
        courseAdapter.setOnItemClickListener(course -> {
            AddCourseDialog dialog = AddCourseDialog.newInstance(course);
            dialog.show(getParentFragmentManager(), "EditCourseDialog");
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Khi quay lại màn hình, đảm bảo load dữ liệu cho Tab đang chọn hiện tại
//        int selectedIndex = tabLayout.getSelectedTabPosition();
//        if (selectedIndex != -1) {
//            TabLayout.Tab currentTab = tabLayout.getTabAt(selectedIndex);
//            if (currentTab != null && currentTab.getText() != null) {
//                loadCoursesByDay(currentTab.getText().toString());
//            }
//        } else {
//            selectTodayTab();
//        }
    }

    //Hàm thiết lập Tab và logic chọn ngày
    private void setupTabs() {
        // Thêm các tab vào TabLayout
        for (String day : daysOfWeek) {
            tabLayout.addTab(tabLayout.newTab().setText(day));
        }

        // Lắng nghe sự kiện người dùng bấm vào Tab
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // Khi tab được chọn -> Load dữ liệu từ DB theo tên ngày
                if (tab.getText() != null) {
                    loadCoursesByDay(tab.getText().toString());
                }
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        selectTodayTab();
    }

    //  Hàm tính toán và chọn Tab ngày hiện tại
    private void selectTodayTab() {
        int today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        // Calendar.SUNDAY = 1, MONDAY = 2, ...
        // Logic mapping:
        // CN (1) -> index 6
        // T2 (2) -> index 0
        int index = (today == Calendar.SUNDAY) ? 6 : today - 2;

        if (index < 0) index = 0; // Phòng lỗi

        // Lấy tab tại vị trí index và chọn nó
        // Việc gọi .select() sẽ kích hoạt listener onTabSelected ở trên -> tự động load data
        TabLayout.Tab tab = tabLayout.getTabAt(index);
        if (tab != null) {
            tab.select();
        }
    }
    @SuppressLint("NotifyDataSetChanged")
    private void loadCoursesByDay(String day){
        // Nếu đã có LiveData cũ, gỡ bỏ Observer khỏi nó.
        if (currentLiveData != null) {
            currentLiveData.removeObservers(getViewLifecycleOwner());
        }
        // Lấy LiveData mới và lưu vào biến
        currentLiveData = db.eventDao().getCoursesByDay(day);

        //  Gắn Observer mới vào LiveData mới
        currentLiveData.observe(getViewLifecycleOwner(), courses -> {
            courseList.clear();
            if (courses != null) {
                courseList.addAll(courses);
            }
            courseAdapter.notifyDataSetChanged();
        });
    }

}
