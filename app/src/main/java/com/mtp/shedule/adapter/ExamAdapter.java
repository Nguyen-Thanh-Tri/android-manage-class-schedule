package com.mtp.shedule.adapter;

import static com.mtp.shedule.SelectColorDialog.COLOR_MAPPING_DRAWABLE;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.mtp.shedule.R;
import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.ExamEntity;
import java.util.List;

public class ExamAdapter extends RecyclerView.Adapter<ExamAdapter.ExamViewHolder> {
    private List<ExamEntity> examList;
    private Context context;
    private ConnDatabase db;

    public ExamAdapter(Context context, List<ExamEntity> examList) {
        this.context = context;
        this.examList = examList;
        this.db = ConnDatabase.getInstance(context);
    }

    @NonNull
    @Override
    public ExamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_exam, parent, false);
        return new ExamViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull ExamViewHolder holder, int position) {
        ExamEntity exam = examList.get(position);
        holder.tvSubject.setText(exam.getSubject());
        holder.tvDate.setText(exam.getDate());
        holder.tvTime.setText(exam.getTime());
        holder.tvLocation.setText(exam.getLocation());

        // Tap to edit
        holder.itemView.setOnClickListener(v -> {
            androidx.fragment.app.FragmentActivity activity = (androidx.fragment.app.FragmentActivity) context;
            com.mtp.shedule.AddExamDialog dialog = new com.mtp.shedule.AddExamDialog();
            Bundle args = new Bundle();
            args.putInt("id", exam.getId());
            args.putString("subject", exam.getSubject());
            args.putString("date", exam.getDate());
            args.putString("time", exam.getTime());
            args.putString("location", exam.getLocation());
            dialog.setArguments(args);
            dialog.show(activity.getSupportFragmentManager(), "EditExamDialog");
        });

        // Hold to delete
        holder.itemView.setOnLongClickListener(v -> {
            new android.app.AlertDialog.Builder(context)
                .setTitle("Delete Exam")
                .setMessage("Are you sure you want to delete this exam?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    new Thread(() -> {
                        db.examDao().delete(exam);
                        ((android.app.Activity) context).runOnUiThread(() -> {
                            examList.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, examList.size());
                            android.widget.Toast.makeText(context, "Exam deleted", android.widget.Toast.LENGTH_SHORT).show();
                        });
                    }).start();
                })
                .setNegativeButton("Cancel", null)
                .show();
            return true;
        });

        int colorIndex = exam.getColor();
        int drawableResId;

        if (colorIndex >= 0 && colorIndex < COLOR_MAPPING_DRAWABLE.length) {
            drawableResId = COLOR_MAPPING_DRAWABLE[colorIndex];
        } else {
            drawableResId = COLOR_MAPPING_DRAWABLE[0];
        }
        holder.cardView.setBackgroundResource(drawableResId);
    }


    
    @Override
    public int getItemCount() {
        return examList.size();
    }

    public static class ExamViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubject, tvDate, tvTime, tvLocation;
        CardView cardView;

        public ExamViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tvExamSubject);
            tvDate = itemView.findViewById(R.id.tvExamDate);
            tvTime = itemView.findViewById(R.id.tvExamTime);
            tvLocation = itemView.findViewById(R.id.tvExamLocation);
            cardView = itemView.findViewById(R.id.cardViewItemExam);
        }
    }
}
