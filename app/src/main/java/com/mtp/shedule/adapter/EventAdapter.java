package com.mtp.shedule.adapter;

import static com.mtp.shedule.SelectColorDialog.COLOR_MAPPING_DRAWABLE;

import android.annotation.SuppressLint;
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
import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.EventEntity;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {

    private final List<EventEntity> eventList;
    private final Context context;
    final ConnDatabase db;


    public EventAdapter(Context context, List<EventEntity> eventList) {
        this.context = context;
        this.eventList = eventList;
        this.db = ConnDatabase.getInstance(context);
    }

    public interface OnItemClickListener {
        void onItemClick(EventEntity event);
    }

    private OnItemClickListener listener;

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_event_card, parent, false);
        return new EventViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        EventEntity event = eventList.get(position);
        holder.tvTitle.setText(event.getTitle());
        holder.tvTime.setText(event.getStartTimeFormatted() + "   " + event.getEndTimeFormatted());

        // click item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(event);
        });

        // long click delete
        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(v.getContext())
                    .setTitle("Delete Event")
                    .setMessage("Delete \"" + event.getTitle() + "\"?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        new Thread(() -> {
                            db.eventDao().deleteEvent(event);
                            holder.itemView.post(() -> {
                                eventList.remove(holder.getAdapterPosition());
                                notifyItemRemoved(holder.getAdapterPosition());
                                Toast.makeText(v.getContext(), "Deleted", Toast.LENGTH_SHORT).show();
                            });
                        }).start();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

        int colorIndex = event.getColor();
        int drawableResId;

        if (colorIndex >= 0 && colorIndex < COLOR_MAPPING_DRAWABLE.length) {
            drawableResId = COLOR_MAPPING_DRAWABLE[colorIndex];
        } else {
            drawableResId = COLOR_MAPPING_DRAWABLE[0];
        }
        // Set background color
        holder.colorIndicator.setBackgroundResource(drawableResId);
    }

    @Override
    public int getItemCount() {
        return eventList.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvTime;
        CardView cardView;
        View colorIndicator;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvTime = itemView.findViewById(R.id.tvTime);
            cardView = (CardView) itemView;
            colorIndicator = itemView.findViewById(R.id.color_indicator);
        }
    }
}
