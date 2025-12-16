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
import com.mtp.shedule.fragment.calendarfragment.MonthViewFragment;

import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.EventViewHolder> {
    private OnEventDeletedListener listener1;
    private final List<EventEntity> eventList;
    private final Context context;
    final ConnDatabase db;


    public EventAdapter(Context context, List<EventEntity> eventList) {
        this.context = context;
        this.eventList = eventList;
        this.db = ConnDatabase.getInstance(context);
    }

    public interface OnEventDeletedListener {
        void onEventDeleted();
    }
    public void setOnEventDeletedListener(OnEventDeletedListener listener1) {
        this.listener1 = listener1;
    }

    public interface OnItemClickListener {
        void onItemClick(EventEntity event);
    }

    OnItemClickListener listener;

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
            String deleteMessage;
            String eventType;

            // Determine if this is a course instance or regular event
            if (event.getId() < 0) {
                // This is a generated instance of a repeating event
                deleteMessage = "Delete all instances of course \"" + event.getTitle() + "\"?\n\nThis will remove the course from all weeks.";
                eventType = "course";
            } else if (event.isCourse() && "weekly".equals(event.getRepeatType())) {
                // This is an original repeating event
                deleteMessage = "Delete all instances of course \"" + event.getTitle() + "\"?\n\nThis will remove the course from all weeks.";
                eventType = "course";
            } else {
                // This is a regular one-time event
                deleteMessage = "Delete event \"" + event.getTitle() + "\"?";
                eventType = "event";
            }

            new AlertDialog.Builder(v.getContext())
                    .setTitle("Delete " + (eventType.equals("course") ? "Course" : "Event"))
                    .setMessage(deleteMessage)
                    .setPositiveButton("Delete", (dialog, which) -> {
                        new Thread(() -> {
                            try {
                                if (event.getId() < 0) {
                                    // Delete the original repeating event (positive ID)
                                    int originalId = Math.abs(event.getId());
                                    EventEntity originalEvent = db.eventDao().getEventById(originalId);
                                    if (originalEvent != null) {
                                        db.eventDao().deleteEvent(originalEvent);
                                    }
                                } else {
                                    // Delete the regular event or original repeating event
                                    db.eventDao().deleteEvent(event);
                                }

                                holder.itemView.post(() -> {
                                    eventList.remove(holder.getAdapterPosition());
                                    notifyItemRemoved(holder.getAdapterPosition());
                                    Toast.makeText(v.getContext(), "Deleted", Toast.LENGTH_SHORT).show();

                                    // Notify parent fragment to refresh the calendar
                                    // This ensures other views update as well
                                    if (context instanceof androidx.fragment.app.FragmentActivity) {
                                        ((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager()
                                            .setFragmentResult("event_deleted", new android.os.Bundle());
                                    }
                                });
                            } catch (Exception e) {
                                e.printStackTrace();
                                holder.itemView.post(() -> {
                                    Toast.makeText(v.getContext(), "Error deleting event", Toast.LENGTH_SHORT).show();
                                });
                            }
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
