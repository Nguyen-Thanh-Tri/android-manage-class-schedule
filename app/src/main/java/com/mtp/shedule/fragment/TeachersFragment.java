package com.mtp.shedule.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mtp.shedule.AddTeacherDialog;
import com.mtp.shedule.adapter.TeacherAdapter;
import com.mtp.shedule.R;
import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.TeacherEntity;

import java.util.ArrayList;
import java.util.List;

public class TeachersFragment extends Fragment {

    private RecyclerView recyclerView;
    private TeacherAdapter teacherAdapter;
    private final List<TeacherEntity> teacherList = new ArrayList<>();
    private FloatingActionButton fabAddTeacher;
    private ConnDatabase db;

    public TeachersFragment() {
        // Required empty constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher, container, false);

        recyclerView = view.findViewById(R.id.recyclerTeachers);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        teacherAdapter = new TeacherAdapter(getContext(), teacherList);
        recyclerView.setAdapter(teacherAdapter);

        db = ConnDatabase.getInstance(requireContext());

        fabAddTeacher = view.findViewById(R.id.fabAddTeacher);
        fabAddTeacher.setOnClickListener(v -> {
            new AddTeacherDialog().show(getParentFragmentManager(), "AddTeacherDialog");
        });

        // Observe teachers from database
        db.teacherDao().getAllTeachers().observe(getViewLifecycleOwner(), teachers -> {
            teacherList.clear();
            if (teachers != null) teacherList.addAll(teachers);
            teacherAdapter.notifyDataSetChanged();
        });

        // Lắng nghe kết quả từ Dialog (thêm/cập nhật)
        getParentFragmentManager().setFragmentResultListener("teacher_added", getViewLifecycleOwner(),
                (requestKey, bundle) -> teacherAdapter.notifyDataSetChanged());

        return view;
    }
}
