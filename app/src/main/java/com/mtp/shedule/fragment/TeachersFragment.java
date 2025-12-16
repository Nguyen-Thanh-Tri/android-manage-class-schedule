package com.mtp.shedule.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;

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

import java.util.Objects;

public class TeachersFragment extends Fragment {

    private RecyclerView recyclerView;
    private TeacherAdapter teacherAdapter;
    private FloatingActionButton fabAddTeacher;
    private ConnDatabase db;
    private EditText edtSearchTeacher;
    private ImageButton btnSort;

    public TeachersFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_teacher, container, false);

        recyclerView = view.findViewById(R.id.recyclerTeachers);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        teacherAdapter = new TeacherAdapter(requireContext());
        recyclerView.setAdapter(teacherAdapter);

        db = ConnDatabase.getInstance(requireContext());

        // FAB add teacher
        fabAddTeacher = view.findViewById(R.id.fabAddTeacher);
        fabAddTeacher.setOnClickListener(v -> new AddTeacherDialog()
                .show(getParentFragmentManager(), "AddTeacherDialog"));

        // Observe teachers from database
        db.teacherDao().getAllTeachers().observe(getViewLifecycleOwner(), teachers -> {
            if (teachers != null) teacherAdapter.updateList(teachers);
        });

        // Search using EditText
        edtSearchTeacher = view.findViewById(R.id.edtSearchTeacher);
        edtSearchTeacher.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                teacherAdapter.getFilter().filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Sort button -> popup menu
        btnSort = view.findViewById(R.id.btnSort);
        btnSort.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(requireContext(), v);
            popup.getMenu().add(0, 0, 0, "Sort A → Z");
            popup.getMenu().add(0, 1, 1, "Sort Z → A");
            popup.setOnMenuItemClickListener(item -> {
                teacherAdapter.sortByName(item.getItemId() == 0);
                return true;
            });
            popup.show();
        });

        return view;
    }
}
