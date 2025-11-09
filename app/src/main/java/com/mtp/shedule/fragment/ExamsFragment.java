package com.mtp.shedule.fragment;

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

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mtp.shedule.adapter.ExamAdapter;
import com.mtp.shedule.R;
import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.ExamEntity;

import java.util.ArrayList;
import java.util.List;

public class ExamsFragment extends Fragment {

    private RecyclerView recyclerView;
    private ExamAdapter examAdapter;
    private ConnDatabase db;
    private final List<ExamEntity> examList = new ArrayList<>();
    private FloatingActionButton fabAddExam;

    public ExamsFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_exams, container, false);

        recyclerView = view.findViewById(R.id.recyclerViewExams);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        examAdapter = new ExamAdapter(getContext(), examList);
        recyclerView.setAdapter(examAdapter);

        db = ConnDatabase.getInstance(getContext());

        fabAddExam = view.findViewById(R.id.fabAddExam);

        fabAddExam.setOnClickListener(v -> {
            new com.mtp.shedule.AddExamActivity().show(getParentFragmentManager(), "AddExamDialog");
        });

        // Observe exams from database
        db.examDao().getAllExams().observe(getViewLifecycleOwner(), exams -> {
            examList.clear();
            examList.addAll(exams);
            examAdapter.notifyDataSetChanged();
        });

        return view;
    }
}
