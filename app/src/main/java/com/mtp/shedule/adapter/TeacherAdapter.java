package com.mtp.shedule.adapter;

import static com.mtp.shedule.SelectColorDialog.COLOR_MAPPING_DRAWABLE;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.mtp.shedule.R;
import com.mtp.shedule.entity.TeacherEntity;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TeacherAdapter extends RecyclerView.Adapter<TeacherAdapter.TeacherViewHolder> implements Filterable {

    private final Context context;
    private List<TeacherEntity> teacherList = new ArrayList<>();
    private List<TeacherEntity> teacherListFull = new ArrayList<>();
    private OnItemClickListener listener;
    private boolean isAscending = true;

    public interface OnItemClickListener {
        void onItemClick(TeacherEntity teacher);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    public interface OnItemLongClickListener {
        void onItemLongClick(TeacherEntity teacher);
    }

    private OnItemLongClickListener longClickListener;

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }

    public TeacherAdapter(Context context) {
        this.context = context;
    }

    public void updateList(List<TeacherEntity> newList) {
        this.teacherListFull = new ArrayList<>(newList);
        // Áp dụng sắp xếp hiện tại cho danh sách mới
        sortList(this.teacherListFull, isAscending);
        this.teacherList = new ArrayList<>(this.teacherListFull);
        notifyDataSetChanged();
    }
    public void sortByName(boolean ascending) {
        this.isAscending = ascending;
        sortList(teacherListFull, ascending);
        sortList(teacherList, ascending);
        notifyDataSetChanged();
    }
    private void sortList(List<TeacherEntity> list, boolean ascending) {
        Collections.sort(list, (t1, t2) -> {
            String name1 = t1.getName() == null ? "" : t1.getName();
            String name2 = t2.getName() == null ? "" : t2.getName();
            return ascending ? name1.compareToIgnoreCase(name2) : name2.compareToIgnoreCase(name1);
        });
    }
    @NonNull
    @Override
    public TeacherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_teacher, parent, false);
        return new TeacherViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull TeacherViewHolder holder, int position) {
        TeacherEntity t = teacherList.get(position);
        holder.tvName.setText(t.getName());
        holder.tvPosition.setText(t.getPosition());
        holder.tvPhone.setText(t.getPhone());
        holder.tvEmail.setText(t.getEmail());
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onItemLongClick(t);
                return true;
            }
            return false;
        });

        int colorIdx = t.getColor();
        holder.cardView.setBackgroundResource(COLOR_MAPPING_DRAWABLE[colorIdx]);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(t);
        });
    }

    @Override
    public int getItemCount() { return teacherList.size(); }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                List<TeacherEntity> filtered = new ArrayList<>();
                if (constraint == null || constraint.length() == 0) {
                    filtered.addAll(teacherListFull);
                } else {
                    String pattern = constraint.toString().toLowerCase().trim();
                    for (TeacherEntity item : teacherListFull) {
                        if (item.getName().toLowerCase().contains(pattern)) filtered.add(item);
                    }
                }
                FilterResults res = new FilterResults();
                res.values = filtered;
                return res;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                teacherList.clear();
                teacherList.addAll((List) results.values);
                notifyDataSetChanged();
            }
        };
    }

    static class TeacherViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvPosition, tvPhone, tvEmail;
        CardView cardView;
        public TeacherViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvTeacherName);
            tvPosition = itemView.findViewById(R.id.tvTeacherPosition);
            tvPhone = itemView.findViewById(R.id.tvTeacherPhone);
            tvEmail = itemView.findViewById(R.id.tvTeacherEmail);
            cardView = itemView.findViewById(R.id.cardViewItemTeacher);
        }
    }
}