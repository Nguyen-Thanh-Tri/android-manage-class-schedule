package com.mtp.shedule;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TeacherAdapter extends RecyclerView.Adapter<TeacherAdapter.TeacherViewHolder> {

    private Context context;
    private List<Teacher> teacherList;

    public TeacherAdapter(Context context, List<Teacher> teacherList) {
        this.context = context;
        this.teacherList = teacherList;
    }

    @NonNull
    @Override
    public TeacherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_teacher, parent, false);
        return new TeacherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeacherViewHolder holder, int position) {
        Teacher teacher = teacherList.get(position);

        holder.tvName.setText(teacher.getName());
        holder.tvPosition.setText(teacher.getPosition());
        holder.tvPhone.setText(teacher.getPhone());
        holder.tvEmail.setText(teacher.getEmail());

        // ✅ Đặt màu nền cho toàn card
        holder.cardView.setCardBackgroundColor(context.getResources().getColor(teacher.getColorResId()));
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
            cardView = (CardView) itemView;
            tvName = itemView.findViewById(R.id.tvTeacherName);
            tvPosition = itemView.findViewById(R.id.tvTeacherPosition);
            tvPhone = itemView.findViewById(R.id.tvTeacherPhone);
            tvEmail = itemView.findViewById(R.id.tvTeacherEmail);
        }
    }
}
