package com.mtp.shedule;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {

    public List<Course> courseList;
    public Context context;

    public CourseAdapter(Context context, List<Course> courseList) {
        this.context = context;
        this.courseList = courseList;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_course, parent, false);
        return new CourseViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course course = courseList.get(position);
        holder.tvCourseTitle.setText(course.getTitle());
        holder.tvTeacher.setText(course.getTeacher());
        holder.tvRoom.setText(course.getRoom());
        holder.tvTime.setText(course.getTimeStart() + " - " + course.getTimeEnd());


        //giữ để xóa
        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("DELETE")
                    .setMessage("Are you sure? \"" + course.getTitle() + "\" No?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        DatabaseHelper db = new DatabaseHelper(v.getContext());
                        db.deleteCourse(course.getId()); // Xóa khỏi DB

                        // Xóa khỏi danh sách hiện tại và cập nhật RecyclerView
                        courseList.remove(holder.getAdapterPosition());
                        notifyItemRemoved(holder.getAdapterPosition());

                        Toast.makeText(v.getContext(), "Delete successfully", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true; // Đã xử lý long click
        });
    }

    @Override
    public int getItemCount() {
        return courseList.size();
    }



    public static class CourseViewHolder extends RecyclerView.ViewHolder {
        TextView tvCourseTitle, tvTeacher, tvRoom, tvTime;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseTitle = itemView.findViewById(R.id.tvCourseTitle);
            tvTeacher = itemView.findViewById(R.id.tvTeacher);
            tvRoom = itemView.findViewById(R.id.tvRoom);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }
}
