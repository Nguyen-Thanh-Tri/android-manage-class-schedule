package com.mtp.shedule.adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.mtp.shedule.R;
import com.mtp.shedule.dao.TeacherDao;
import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.TeacherEntity;

import java.util.List;

public class TeacherAdapter extends RecyclerView.Adapter<TeacherAdapter.TeacherViewHolder> {

    private Context context;
    private ConnDatabase db;
    private List<TeacherEntity> teacherList;


    public TeacherAdapter(Context context, List<TeacherEntity> teacherList) {
        this.context = context;
        this.teacherList = teacherList;
        this.db = ConnDatabase.getInstance(context);
    }

    @NonNull
    @Override
    public TeacherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_teacher, parent, false);
        return new TeacherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeacherViewHolder holder, int position) {
        TeacherEntity teacher = teacherList.get(position);

        holder.tvName.setText(teacher.getName());
        holder.tvPosition.setText(teacher.getPosition());
        holder.tvPhone.setText(teacher.getPhone());
        holder.tvEmail.setText(teacher.getEmail());

        // ✅ Đặt màu nền cho toàn card
        holder.cardView.setBackgroundResource(teacher.getColorResId());


        //giữ để xóa
        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("DELETE")
                    .setMessage("Are you sure? \"" + teacher.getName() + "\" No?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        new Thread(() -> {
                            db.teacherDao().deleteTeacher(teacher); // ← Entity
                            holder.itemView.post(() -> {
                                teacherList.remove(holder.getAdapterPosition());
                                notifyItemRemoved(holder.getAdapterPosition());
                                Toast.makeText(v.getContext(), "Deleted successfully", Toast.LENGTH_SHORT).show();
                            });
                        }).start();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true; // Đã xử lý long click
        });
    }

    @Override
    public int getItemCount() {
        return teacherList.size();
    }

    public static class TeacherViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPosition, tvPhone, tvEmail;
        CardView cardView;

        public TeacherViewHolder(@NonNull View itemView) {
            super(itemView);

            cardView = itemView.findViewById(R.id.cardView);
            tvName = itemView.findViewById(R.id.tvTeacherName);
            tvPosition = itemView.findViewById(R.id.tvTeacherPosition);
            tvPhone = itemView.findViewById(R.id.tvTeacherPhone);
            tvEmail = itemView.findViewById(R.id.tvTeacherEmail);
        }
    }
}
