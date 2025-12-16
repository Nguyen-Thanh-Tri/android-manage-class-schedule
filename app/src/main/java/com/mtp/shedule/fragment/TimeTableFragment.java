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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mtp.shedule.AddCourseDialog;
import com.mtp.shedule.adapter.CourseAdapter;
import com.mtp.shedule.R;
import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.CourseEntity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TimeTableFragment extends Fragment {

    private RecyclerView recyclerView;
    private CourseAdapter courseAdapter;
    private ConnDatabase db;
    private final List<CourseEntity> courseList = new ArrayList<>();

    private FloatingActionButton fabAdd;
    private MaterialButton[] dayButtons;
    private LiveData<List<CourseEntity>> currentLiveData;

    public TimeTableFragment() {

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


        // Day buttons
        dayButtons = new MaterialButton[]{
                view.findViewById(R.id.btnMonday),
                view.findViewById(R.id.btnTuesday),
                view.findViewById(R.id.btnWednesday),
                view.findViewById(R.id.btnThursday),
                view.findViewById(R.id.btnFriday),
                view.findViewById(R.id.btnSaturday),
                view.findViewById(R.id.btnSunday)
        };


        // Highlight today
        int today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        int index = (today == Calendar.SUNDAY) ? 6 : today - 2;
        highlightDay(index);
        loadCoursesByDay(dayButtons[index].getText().toString());


// ---------------- Floating Action Button -Thêm lịch học ----------------
        fabAdd = view.findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            AddCourseDialog dialog = new AddCourseDialog();
            dialog.show(getParentFragmentManager(), "AddCourseDialog");
        });

        courseAdapter.setOnItemClickListener(course -> {
            AddCourseDialog dialog = AddCourseDialog.newInstance(course);
            dialog.show(getParentFragmentManager(), "EditCourseDialog");
        });

        // Click listeners for day buttons
        for (int i = 0; i < dayButtons.length; i++) {
            int finalI = i;
            dayButtons[i].setOnClickListener(v -> {
                highlightDay(finalI);
                loadCoursesByDay(dayButtons[finalI].getText().toString());
            });
        }

        return view;
    }

    private void highlightDay(int index){
        for(int i = 0; i < dayButtons.length; i++){
            if(i == index){
                dayButtons[i].setBackgroundColor(getResources().getColor(R.color.orange, null));
                dayButtons[i].setTextColor(getResources().getColor(android.R.color.white, null));
            } else {
                dayButtons[i].setBackgroundColor(getResources().getColor(android.R.color.transparent, null));
                dayButtons[i].setTextColor(getResources().getColor(android.R.color.black, null));
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadCoursesByDay(String day){
        // Nếu đã có LiveData cũ, gỡ bỏ Observer khỏi nó.
        if (currentLiveData != null) {
            currentLiveData.removeObservers(getViewLifecycleOwner());
        }
        // Lấy LiveData mới và lưu vào biến
        currentLiveData = db.courseDao().getCoursesByDay(day);

        //  Gắn Observer mới vào LiveData mới
        currentLiveData.observe(getViewLifecycleOwner(), courses -> {
            courseList.clear();
            courseList.addAll(courses);
            courseAdapter.notifyDataSetChanged();
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        int today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        int index = (today == Calendar.SUNDAY) ? 6 : today - 2;
        highlightDay(index);
//        loadCoursesByDay(dayButtons[index].getText().toString());
    }
}
