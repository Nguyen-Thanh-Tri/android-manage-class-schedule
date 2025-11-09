package com.mtp.shedule.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
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
    }


    
    @Override
    public int getItemCount() {
        return examList.size();
    }

    public static class ExamViewHolder extends RecyclerView.ViewHolder {
        TextView tvSubject, tvDate, tvTime, tvLocation;
        public ExamViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSubject = itemView.findViewById(R.id.tvExamSubject);
            tvDate = itemView.findViewById(R.id.tvExamDate);
            tvTime = itemView.findViewById(R.id.tvExamTime);
            tvLocation = itemView.findViewById(R.id.tvExamLocation);
        }
    }
}
