package com.mtp.shedule;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TimeTableFragment extends Fragment {

    private RecyclerView recyclerView;
    private CourseAdapter courseAdapter;
    private DatabaseHelper dbHelper;
    private List<Course> courseList = new ArrayList<>();

    private FloatingActionButton fabAdd;
    private MaterialButton[] dayButtons;

    public TimeTableFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timetable, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewCourses);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        dbHelper = new DatabaseHelper(getContext());

        courseAdapter = new CourseAdapter(getContext(), courseList);
        recyclerView.setAdapter(courseAdapter);


        // ---------------- Floating Action Button ----------------
        fabAdd = view.findViewById(R.id.fabAdd);
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddCourseActivity.class);
            startActivity(intent);
        });

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
                dayButtons[i].setBackgroundColor(getResources().getColor(R.color.orange));
                dayButtons[i].setTextColor(getResources().getColor(android.R.color.white));
            } else {
                dayButtons[i].setBackgroundColor(getResources().getColor(android.R.color.transparent));
                dayButtons[i].setTextColor(getResources().getColor(android.R.color.black));
            }
        }
    }

    private void loadCoursesByDay(String day){
        courseList.clear();
        courseList.addAll(dbHelper.getCoursesByDay(day));
        courseAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        int today = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        int index = (today == Calendar.SUNDAY) ? 6 : today - 2;
        loadCoursesByDay(dayButtons[index].getText().toString());
    }
}
