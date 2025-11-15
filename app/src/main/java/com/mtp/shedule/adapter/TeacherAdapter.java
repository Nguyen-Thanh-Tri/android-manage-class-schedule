package com.mtp.shedule.adapter;

import static com.mtp.shedule.SelectColorDialog.COLOR_MAPPING_DRAWABLE;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.mtp.shedule.AddTeacherDialog;
import com.mtp.shedule.R;
import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.TeacherEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TeacherAdapter extends RecyclerView.Adapter<TeacherAdapter.TeacherViewHolder> implements Filterable {

    private final Context context;
    private final List<TeacherEntity> teacherList = new ArrayList<>();      // danh sách hiển thị
    private final List<TeacherEntity> teacherListFull = new ArrayList<>();  // danh sách đầy đủ (nguồn truth)
    private final ConnDatabase db;
    private String searchQuery = "";

    public TeacherAdapter(Context context) {
        this.context = context;
        this.db = ConnDatabase.getInstance(context);
    }

    /**
     * Gọi từ Fragment khi dữ liệu từ Room (LiveData) thay đổi.
     * Cập nhật cả teacherList và teacherListFull.
     */
    public void updateList(List<TeacherEntity> newList) {
        teacherList.clear();
        if (newList != null) teacherList.addAll(newList);

        teacherListFull.clear();
        if (newList != null) teacherListFull.addAll(newList);

        // Nếu có search đang hoạt động, apply filter lại để hiển thị chính xác
        if (searchQuery != null && !searchQuery.isEmpty()) {
            getFilter().filter(searchQuery);
        } else {
            notifyDataSetChanged();
        }
    }

    // ---------------- FILTER (SEARCH) ----------------
    @Override
    public Filter getFilter() {
        return teacherFilter;
    }

    private final Filter teacherFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<TeacherEntity> filteredList = new ArrayList<>();
            String query = (constraint == null) ? "" : constraint.toString().toLowerCase().trim();

            if (query.isEmpty()) {
                filteredList.addAll(teacherListFull);
            } else {
                for (TeacherEntity teacher : teacherListFull) {
                    String name = teacher.getName() == null ? "" : teacher.getName().toLowerCase();
                    String pos = teacher.getPosition() == null ? "" : teacher.getPosition().toLowerCase();
                    String phone = teacher.getPhone() == null ? "" : teacher.getPhone().toLowerCase();
                    String email = teacher.getEmail() == null ? "" : teacher.getEmail().toLowerCase();

                    if (name.contains(query) || pos.contains(query) || phone.contains(query) || email.contains(query)) {
                        filteredList.add(teacher);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            results.count = filteredList.size();
            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            searchQuery = (constraint == null) ? "" : constraint.toString().toLowerCase().trim();

            teacherList.clear();
            if (results != null && results.values != null) {
                teacherList.addAll((List<TeacherEntity>) results.values);
            }
            notifyDataSetChanged();
        }
    };

    // ---------------- SORT ----------------
    public void sortByName(boolean ascending) {
        Comparator<TeacherEntity> cmp = Comparator.comparing(t -> t.getName() == null ? "" : t.getName(), String.CASE_INSENSITIVE_ORDER);
        if (!ascending) {
            cmp = cmp.reversed();
        }

        Collections.sort(teacherListFull, cmp);
        Collections.sort(teacherList, cmp);
        notifyDataSetChanged();
    }


    // ---------------- ADAPTER CORE ----------------
    @NonNull
    @Override
    public TeacherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_teacher, parent, false);
        return new TeacherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeacherViewHolder holder, int position) {
        if (position < 0 || position >= teacherList.size()) return;
        TeacherEntity teacher = teacherList.get(position);

        holder.tvName.setText(teacher.getName());
        holder.tvPosition.setText(teacher.getPosition());
        holder.tvPhone.setText(teacher.getPhone());
        holder.tvEmail.setText(teacher.getEmail());

        int colorIndex = teacher.getColor();
        holder.cardView.setBackgroundResource(
                (colorIndex >= 0 && colorIndex < COLOR_MAPPING_DRAWABLE.length)
                        ? COLOR_MAPPING_DRAWABLE[colorIndex]
                        : COLOR_MAPPING_DRAWABLE[0]
        );

        // Click để edit -> mở dialog với args
        holder.itemView.setOnClickListener(v -> {
            AddTeacherDialog dialog = new AddTeacherDialog();
            android.os.Bundle args = new android.os.Bundle();
            args.putInt("id", teacher.getId());
            args.putString("name", teacher.getName());
            args.putString("position", teacher.getPosition());
            args.putString("phone", teacher.getPhone());
            args.putString("email", teacher.getEmail());
            args.putInt("colorIndex", teacher.getColor());
            dialog.setArguments(args);
            dialog.show(((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager(), "EditTeacherDialog");
        });

        // Long click: delete
        holder.itemView.setOnLongClickListener(v -> {
            new android.app.AlertDialog.Builder(context)
                    .setTitle("Delete Teacher")
                    .setMessage("Delete \"" + (teacher.getName() == null ? "" : teacher.getName()) + "\"?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        // Xóa trong background thread
                        new Thread(() -> {
                            db.teacherDao().deleteTeacher(teacher);
                            // Cập nhật UI trên main thread
                            new Handler(Looper.getMainLooper()).post(() -> {
                                // remove teacher từ cả 2 list để đồng bộ
                                teacherList.remove(teacher);
                                teacherListFull.remove(teacher);
                                notifyDataSetChanged();
                                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show();
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
