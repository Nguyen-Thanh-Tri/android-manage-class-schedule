package com.mtp.shedule.fragment;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.mtp.shedule.AddTeacherDialog;
import com.mtp.shedule.R;
import com.mtp.shedule.adapter.TeacherAdapter;
import com.mtp.shedule.database.ConnDatabase;
import android.widget.EditText;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ImageButton;
import android.widget.Toast;

public class TeachersFragment extends Fragment {

    private TeacherAdapter adapter;
    private ConnDatabase db;
    private ImageButton btnSort;
    private boolean isAscending = true;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_teacher, container, false);
        db = ConnDatabase.getInstance(requireContext());

        CoordinatorLayout root = v.findViewById(R.id.coordinatorRoot);
        RecyclerView rv = v.findViewById(R.id.recyclerTeachers);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new TeacherAdapter(requireContext());
        rv.setAdapter(adapter);

        btnSort = v.findViewById(R.id.btnSort);

        // Click để xem chi tiết
        adapter.setOnItemClickListener(teacher ->
                AddTeacherDialog.newInstance(teacher).show(getParentFragmentManager(), "view"));

        // Lắng nghe LiveData
        db.teacherDao().getAllTeachers().observe(getViewLifecycleOwner(), list -> adapter.updateList(list));

        // FAB
        v.findViewById(R.id.fabAddTeacher).setOnClickListener(view ->
                new AddTeacherDialog().show(getParentFragmentManager(), "add"));


        // Ẩn bàn phím khi nhấn vào vùng trống
        root.setOnTouchListener((view, motionEvent) -> {
            hideKeyboard();
            return false;
        });
        // Khi chạm vào danh sách (nhưng không trúng item cụ thể)
        rv.setOnTouchListener((view, event) -> {
            hideKeyboard();
            return false;
        });

        // Search
        EditText search = v.findViewById(R.id.edtSearchTeacher);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { adapter.getFilter().filter(s); }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });


        //SORT
        btnSort.setOnClickListener(view -> {
            // Đảo trạng thái
            isAscending = !isAscending;

            // Thực hiện sắp xếp trong Adapter
            adapter.sortByName(isAscending);

            // Nếu isAscending = true (A-Z): rotation = 0
            // Nếu isAscending = false (Z-A): rotation = 180
            float targetRotation = isAscending ? 0f : 180f;
            btnSort.animate().rotationX(targetRotation).setDuration(300).start();

            // Thông báo cho người dùng
            String msg = isAscending ? "Sorted A → Z" : "Sorted Z → A";
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
        });

        return v;
    }

    private void hideKeyboard() {
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            view.clearFocus();
        }
    }
}