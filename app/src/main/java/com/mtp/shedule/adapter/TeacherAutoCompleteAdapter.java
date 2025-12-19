package com.mtp.shedule.adapter;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.mtp.shedule.entity.TeacherEntity;

import java.util.ArrayList;
import java.util.List;

public class TeacherAutoCompleteAdapter extends ArrayAdapter<String> implements Filterable {
    private List<TeacherEntity> teacherListFull = new ArrayList<>();
    private List<String> teacherNames = new ArrayList<>();
    private List<String> teacherNamesFiltered = new ArrayList<>();

    public TeacherAutoCompleteAdapter(@NonNull Context context) {
        super(context, android.R.layout.simple_dropdown_item_1line);
    }

    public void updateTeachers(List<TeacherEntity> teachers) {
        teacherListFull.clear();
        teacherNames.clear();
        
        if (teachers != null) {
            teacherListFull.addAll(teachers);
            for (TeacherEntity teacher : teachers) {
                teacherNames.add(teacher.getName());
            }
        }
        
        teacherNamesFiltered = new ArrayList<>(teacherNames);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return teacherNamesFiltered.size();
    }

    @Nullable
    @Override
    public String getItem(int position) {
        return teacherNamesFiltered.get(position);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                List<String> suggestions = new ArrayList<>();

                if (constraint == null || constraint.length() == 0) {
                    suggestions.addAll(teacherNames);
                } else {
                    String filterPattern = constraint.toString().toLowerCase().trim();
                    for (String teacher : teacherNames) {
                        if (teacher.toLowerCase().contains(filterPattern)) {
                            suggestions.add(teacher);
                        }
                    }
                }

                results.values = suggestions;
                results.count = suggestions.size();
                return results;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                teacherNamesFiltered.clear();
                if (results.values != null) {
                    teacherNamesFiltered.addAll((List<String>) results.values);
                }
                notifyDataSetChanged();
            }
        };
    }

    // Helper method to get the TeacherEntity by name
    public TeacherEntity getTeacherByName(String name) {
        for (TeacherEntity teacher : teacherListFull) {
            if (teacher.getName().equals(name)) {
                return teacher;
            }
        }
        return null;
    }
}