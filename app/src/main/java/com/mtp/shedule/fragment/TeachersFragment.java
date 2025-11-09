package com.mtp.shedule.fragment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.mtp.shedule.R;
import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.TeacherEntity;
import com.mtp.shedule.adapter.TeacherAdapter;
import com.mtp.shedule.AddTeacherActivity;

import java.util.ArrayList;
import java.util.List;
import android.content.Intent;


public class TeachersFragment extends Fragment {

    private RecyclerView recyclerView;
    private TeacherAdapter teacherAdapter;
    private List<TeacherEntity> teacherList = new ArrayList<>();;
    private Button btnAddTeacher;
    private ConnDatabase db;
    private LiveData<List<TeacherEntity>> currentLiveData;

    public TeachersFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher, container, false);

        recyclerView = view.findViewById(R.id.recyclerTeachers);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        teacherAdapter = new TeacherAdapter(getContext(), teacherList);
        recyclerView.setAdapter(teacherAdapter);

        db = ConnDatabase.getInstance(getContext());

        // Lấy danh sách giảng viên từ cơ sở dữ liệu
        loadTeachers();

        // Sự kiện nút "Add Teacher"
        btnAddTeacher = view.findViewById(R.id.btnAddTeacher);
        btnAddTeacher.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddTeacherActivity.class);
            addTeacherLauncher.launch(intent);
        });


        return view;
    }

    @SuppressLint("NotifyDataSetChanged")
    private void loadTeachers(){
        // Nếu đã có LiveData cũ, gỡ bỏ Observer khỏi nó.
        if (currentLiveData != null) {
            currentLiveData.removeObservers(getViewLifecycleOwner());
        }
        // Lấy LiveData mới từ DAO
        currentLiveData = db.teacherDao().getAllTeachers();
        //  Gắn Observer mới vào LiveData mới
        currentLiveData.observe(getViewLifecycleOwner(), teacher -> {
            teacherList.clear();
            teacherList.addAll(teacher);
            teacherAdapter.notifyDataSetChanged();
        });
    }

    private final ActivityResultLauncher<Intent> addTeacherLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            loadTeachers(); // reload lại danh sách
                        }
                    });

    @Override
    public void onResume() {
        super.onResume();
        loadTeachers();
    }
}
