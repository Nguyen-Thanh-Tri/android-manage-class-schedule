package com.mtp.shedule.adapter;

import static com.mtp.shedule.SelectColorDialog.COLOR_MAPPING_DRAWABLE;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.Filter;
import android.widget.Filterable;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.mtp.shedule.AddTeacherDialog;
import com.mtp.shedule.R;
import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.TeacherEntity;

import java.util.List;

public class TeacherAdapter extends RecyclerView.Adapter<TeacherAdapter.TeacherViewHolder> {

    private final Context context;
    private final List<TeacherEntity> teacherList;
    private final ConnDatabase db;

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

        // Set background color
        int colorIndex = teacher.getColor();
        holder.cardView.setBackgroundResource(
                (colorIndex >= 0 && colorIndex < COLOR_MAPPING_DRAWABLE.length)
                        ? COLOR_MAPPING_DRAWABLE[colorIndex]
                        : COLOR_MAPPING_DRAWABLE[0]
        );

        // Click để edit teacher
        holder.itemView.setOnClickListener(v -> {
            AddTeacherDialog dialog = new AddTeacherDialog();
            Bundle args = new Bundle();
            args.putInt("id", teacher.getId());
            args.putString("name", teacher.getName());
            args.putString("position", teacher.getPosition());
            args.putString("phone", teacher.getPhone());
            args.putString("email", teacher.getEmail());
            args.putInt("colorIndex", teacher.getColor());
            dialog.setArguments(args);
            dialog.show(((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager(), "EditTeacherDialog");
        });

        // Long click để delete
        holder.itemView.setOnLongClickListener(v -> {
            new android.app.AlertDialog.Builder(context)
                    .setTitle("Delete Teacher")
                    .setMessage("Are you sure you want to delete \"" + teacher.getName() + "\"?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        new Thread(() -> {
                            db.teacherDao().deleteTeacher(teacher);
                            ((androidx.fragment.app.FragmentActivity) context).runOnUiThread(() -> {
                                int pos = holder.getAdapterPosition();
                                if (pos != RecyclerView.NO_POSITION) {
                                    teacherList.remove(pos);
                                    notifyItemRemoved(pos);
                                    notifyItemRangeChanged(pos, teacherList.size());
                                    Toast.makeText(context, "Teacher deleted", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }).start();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return teacherList.size();
    }

    static class TeacherViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        TextView tvName, tvPosition, tvPhone, tvEmail;

        public TeacherViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardViewItemTeacher);
            tvName = itemView.findViewById(R.id.tvTeacherName);
            tvPosition = itemView.findViewById(R.id.tvTeacherPosition);
            tvPhone = itemView.findViewById(R.id.tvTeacherPhone);
            tvEmail = itemView.findViewById(R.id.tvTeacherEmail);
        }
    }
}

