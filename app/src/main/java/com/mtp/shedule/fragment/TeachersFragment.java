package com.mtp.shedule.fragment;

import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.mtp.shedule.R;
import com.mtp.shedule.Teacher;
import com.mtp.shedule.adapter.TeacherAdapter;
import com.mtp.shedule.AddTeacherActivity;

import java.util.ArrayList;
import java.util.List;
import android.content.Intent;


public class TeachersFragment extends Fragment {

    private RecyclerView recyclerTeachers;
    private TeacherAdapter teacherAdapter;
    private List<Teacher> teacherList;
    private Button btnAddTeacher;

    public TeachersFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher, container, false);

        recyclerTeachers = view.findViewById(R.id.recyclerTeachers);
        btnAddTeacher = view.findViewById(R.id.btnAddTeacher);

        teacherList = new ArrayList<>();
        teacherList.add(new Teacher("Nguyen Van A", "Math Teacher", "0123456789", "teacherA@gmail.com", R.color.orange));
        teacherList.add(new Teacher("Le Thi B", "English Teacher", "0987654321", "teacherB@gmail.com", R.color.green));
        teacherList.add(new Teacher("Tran Van C", "Physics Teacher", "0911222333", "teacherC@gmail.com", R.color.blue));

        teacherAdapter = new TeacherAdapter(getContext(), teacherList);
        recyclerTeachers.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerTeachers.setAdapter(teacherAdapter);

        // Sự kiện nút "Add Teacher"
        btnAddTeacher.setOnClickListener(v -> {
            // Chuyển sang màn hình AddTeacherActivity
            Intent intent = new Intent(getActivity(), AddTeacherActivity.class);
            startActivity(intent);
        });


        return view;
    }
}
