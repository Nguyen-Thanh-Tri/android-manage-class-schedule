package com.mtp.shedule.fragment.calendarfragment;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;
import androidx.gridlayout.widget.GridLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mtp.shedule.AddEventActivity;
import com.mtp.shedule.R;
import com.mtp.shedule.adapter.EventAdapter;
import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.EventEntity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;

public class MonthViewFragment extends Fragment {
    // Enum to manage the display state of the calendar
    private enum ViewState implements Serializable {
        MONTH_VIEW,
        SPLIT_VIEW,
        WEEK_VIEW
    }
    Set<Integer> daysWithEvents = new HashSet<>();
    private int currentMonthIndex;
    private int currentYear;
    private TextView tvMonthHeader;
    private GridLayout gridFullMonthDays;
    private ImageView ivDragHandle;
    private RecyclerView rvEvents;
    private final List<EventEntity> eventList = new ArrayList<>();
    private EventAdapter eventAdapter;

    private ViewState currentState = ViewState.SPLIT_VIEW; // Default state is balanced
    // Variables for drag logic
    private float initialTouchY;
    private float initialCalendarWeight;
    float initialEventWeight;
    private int rowHeight = 0;
    // Constants
    private static final int DAYS_IN_WEEK = 7;
    private static final int MAX_ROWS = 6;
    private boolean isDragging = false;
    private ConnDatabase db;
    private int actualRowsNeeded = MAX_ROWS;
    // Biến lưu trữ ngày đang được chọn để load event
    Calendar selectedDay = Calendar.getInstance();
    private TextView lastSelectedDayView = null;
    private int selectedDayOfMonth = -1;
    
    // Activity result launcher for AddEventActivity
    private ActivityResultLauncher<Intent> addEventLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Calendar now = Calendar.getInstance();
        currentMonthIndex = now.get(Calendar.MONTH);
        currentYear = now.get(Calendar.YEAR);
        //ngày được chọn ban đầu là ngày hôm nay
        selectedDay.setTime(now.getTime());
        selectedDay.set(Calendar.HOUR_OF_DAY, 0);
        selectedDay.set(Calendar.MINUTE, 0);
        selectedDay.set(Calendar.SECOND, 0);
        selectedDay.set(Calendar.MILLISECOND, 0);

        if (getArguments() != null) {
            currentMonthIndex = getArguments().getInt("INITIAL_MONTH_INDEX", currentMonthIndex);
            currentYear = getArguments().getInt("INITIAL_YEAR", currentYear);
        }
        
        // Initialize activity result launcher for AddEventActivity
        addEventLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                    // Event was created successfully, refresh the calendar
                    refreshEvents();
                }
            }
        );

        if (savedInstanceState != null) {
            currentMonthIndex = savedInstanceState.getInt("state_month_index", currentMonthIndex);
            currentYear = savedInstanceState.getInt("state_year", currentYear);

            // Use the standard getSerializable(String key)
            Object viewStateObject = savedInstanceState.getSerializable("state_view");
            if (viewStateObject instanceof ViewState) {
                currentState = (ViewState) viewStateObject;
            } else {
                currentState = ViewState.SPLIT_VIEW;
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_month_view, container, false);

        rvEvents = view.findViewById(R.id.rvEvents);
        rvEvents.setLayoutManager(new LinearLayoutManager(getContext()));

        eventAdapter = new EventAdapter(getContext(), eventList);
        rvEvents.setAdapter(eventAdapter);
        db = ConnDatabase.getInstance(getContext());

        tvMonthHeader = view.findViewById(R.id.tvMonthHeader);
        gridFullMonthDays = view.findViewById(R.id.gridFullMonthDays);
        ivDragHandle = view.findViewById(R.id.ivDragHandle);

        FloatingActionButton fabAddEvent = view.findViewById(R.id.fabAddEvent);

        setupDragHandle();
        displayCalendar();

        //add event
        fabAddEvent.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddEventActivity.class);
            addEventLauncher.launch(intent);
        });

        //load event
        loadEventsForSelectedDay(
                selectedDay.get(Calendar.DAY_OF_MONTH),
                selectedDay.get(Calendar.MONTH),
                selectedDay.get(Calendar.YEAR)
        );

        // Thiết lập trạng thái ban đầu dựa trên currentState
        view.post(() -> updateViewState(currentState, false));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadEventsForSelectedDay(
                selectedDay.get(Calendar.DAY_OF_MONTH),
                selectedDay.get(Calendar.MONTH),
                selectedDay.get(Calendar.YEAR)
        );
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("state_month_index", currentMonthIndex);
        outState.putInt("state_year", currentYear);
        outState.putSerializable("state_view", currentState);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateMonth(int newMonthIndex, int year) {
        this.currentMonthIndex = newMonthIndex;
        this.currentYear = year;

        Calendar now = Calendar.getInstance();
        boolean isCurrentMonth = (newMonthIndex == now.get(Calendar.MONTH)
                && year == now.get(Calendar.YEAR));

        int newSelectedDay;

        if (isCurrentMonth) {
            // THÁNG HIỆN TẠI → highlight today
            newSelectedDay = now.get(Calendar.DAY_OF_MONTH);
        } else {
            // KHÔNG PHẢI THÁNG HIỆN TẠI → highlight ngày 1
            newSelectedDay = 1;
        }

        // Cập nhật selectedDay HOÀN CHỈNH
        selectedDay.set(year, newMonthIndex, newSelectedDay);
        selectedDay.set(Calendar.HOUR_OF_DAY, 0);
        selectedDay.set(Calendar.MINUTE, 0);
        selectedDay.set(Calendar.SECOND, 0);
        selectedDay.set(Calendar.MILLISECOND, 0);


        if (getView() != null) {
            displayCalendar();

            loadEventsForSelectedDay(newSelectedDay, newMonthIndex, year);

            updateViewState(currentState, false);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupDragHandle() {
        ivDragHandle.setOnClickListener(v -> {
            if (currentState == ViewState.WEEK_VIEW) {
                updateViewState(ViewState.SPLIT_VIEW, true);
            } else {
                updateViewState(ViewState.WEEK_VIEW, true);
            }
        });

        // Simple drag/drop logic
        ivDragHandle.setOnTouchListener((v, event) -> {
            LinearLayout.LayoutParams calendarParams = (LinearLayout.LayoutParams) gridFullMonthDays.getLayoutParams();
            LinearLayout.LayoutParams eventParams = (LinearLayout.LayoutParams) rvEvents.getLayoutParams();

            if (rowHeight == 0 && gridFullMonthDays.getChildCount() > 0) {
                rowHeight = gridFullMonthDays.getHeight() / 6;
            }
            if (rowHeight == 0) return false;

            switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    initialTouchY = event.getRawY();
                    initialCalendarWeight = calendarParams.weight;
                    initialEventWeight = eventParams.weight;
                    isDragging = false;
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float dy = event.getRawY() - initialTouchY;
                    float containerHeight = ((View) gridFullMonthDays.getParent()).getHeight();

                    float DRAG_THRESHOLD = 5f;
                    if (!isDragging) {
                        if (Math.abs(dy) > DRAG_THRESHOLD) {
                            isDragging = true;
                            initialTouchY = event.getRawY();
                            initialCalendarWeight = calendarParams.weight;
                            initialEventWeight = eventParams.weight;
                        } else {
                            return true;
                        }
                    }

                    float weightChange = (dy / containerHeight) * 100f;
                    float newCalendarWeight = initialCalendarWeight + weightChange;
                    float minCalendarWeight = 100f / MAX_ROWS;
                    float maxCalendarWeight = 85f;

                    if (newCalendarWeight < minCalendarWeight) newCalendarWeight = minCalendarWeight;
                    if (newCalendarWeight > maxCalendarWeight) newCalendarWeight = maxCalendarWeight;

                    // Damping để kéo mượt
                    float damping = 0.2f;
                    calendarParams.weight += (newCalendarWeight - calendarParams.weight) * damping;
                    eventParams.weight = 100f - calendarParams.weight;

                    gridFullMonthDays.setLayoutParams(calendarParams);
                    rvEvents.setLayoutParams(eventParams);

                    return true;

                case MotionEvent.ACTION_UP:
                    float deltay = event.getRawY() - initialTouchY;
                    float LIGHT_THRESHOLD = 8f; // Ngưỡng kéo rất nhẹ, dễ trượt để đổi state

                    switch (currentState) {
                        case MONTH_VIEW:
                            if (deltay < -LIGHT_THRESHOLD) {
                                // Kéo nhẹ lên -> về SPLIT_VIEW
                                updateViewState(ViewState.SPLIT_VIEW, true);
                            }
                            break;

                        case SPLIT_VIEW:
                            if (deltay < -LIGHT_THRESHOLD) {
                                // Kéo nhẹ lên -> sang WEEK_VIEW
                                updateViewState(ViewState.WEEK_VIEW, true);
                            } else if (deltay > LIGHT_THRESHOLD) {
                                // Kéo nhẹ xuống -> về MONTH_VIEW
                                updateViewState(ViewState.MONTH_VIEW, true);
                            }
                            break;

                        case WEEK_VIEW:
                            if (deltay > LIGHT_THRESHOLD) {
                                updateViewState(ViewState.SPLIT_VIEW, true);
                            }
                            break;
                    }
                    return true;
            }
            return false;
        });
    }

    private void updateViewState(ViewState newState, boolean animate) {
        currentState = newState;
        float calendarWeight, eventWeight;
        int dragHandleIcon;
        int targetRowIndex = -1;

        if (rowHeight == 0 && gridFullMonthDays.getChildCount() > 0) {
            rowHeight = gridFullMonthDays.getHeight() / 6;
        }

        switch (currentState) {
            case MONTH_VIEW:
                calendarWeight = 100f;
                eventWeight = 0f;
                dragHandleIcon = R.drawable.ic_drag_handle_up;
                break;
            case WEEK_VIEW:
                calendarWeight = (100f / MAX_ROWS)-6.67f;
                eventWeight = 100f - calendarWeight;
                dragHandleIcon = R.drawable.ic_drag_handle_down;

                // đã chọn 1 ô thì ưu tiên lấy hàng từ ô đó
                if (lastSelectedDayView != null) {
                    Object tagObj = lastSelectedDayView.getTag();
                    if (tagObj instanceof int[]) {
                        targetRowIndex = ((int[]) tagObj)[0]; // row
                    }
                }

                // chưa có ô được chọn, và đang xem tháng hiện tại -> tìm ô "istoday"
                if (targetRowIndex == -1) {
                    Calendar todayCal = Calendar.getInstance();
                    int todayDay = todayCal.get(Calendar.DAY_OF_MONTH);
                    int todayMonth = todayCal.get(Calendar.MONTH);
                    int todayYear = todayCal.get(Calendar.YEAR);

                    if (currentMonthIndex == todayMonth && currentYear == todayYear) {
                        targetRowIndex = findRowForDay(todayDay);
                    }
                }

                // Fallback
                if (targetRowIndex == -1) targetRowIndex = 0;
                break;

            case SPLIT_VIEW:
            default:
                calendarWeight = 65f;
                eventWeight = 35f;
                dragHandleIcon = R.drawable.ic_drag_handle_normal;
                break;
        }

        ivDragHandle.setImageResource(dragHandleIcon);

        LinearLayout.LayoutParams calendarParams = (LinearLayout.LayoutParams) gridFullMonthDays.getLayoutParams();
        LinearLayout.LayoutParams eventParams = (LinearLayout.LayoutParams) rvEvents.getLayoutParams();

        if (animate) {
            ValueAnimator animator = ValueAnimator.ofFloat(calendarParams.weight, calendarWeight);
            animator.setDuration(300);
            animator.addUpdateListener(animation -> {
                float animatedWeight = (float) animation.getAnimatedValue();
                calendarParams.weight = animatedWeight;
                eventParams.weight = 100f - animatedWeight;
                gridFullMonthDays.setLayoutParams(calendarParams);
                rvEvents.setLayoutParams(eventParams);

            });
            animator.start();
        } else {
            calendarParams.weight = calendarWeight;
            eventParams.weight = eventWeight;
            gridFullMonthDays.setLayoutParams(calendarParams);
            rvEvents.setLayoutParams(eventParams);
            rvEvents.setVisibility(eventWeight > 0 ? View.VISIBLE : View.GONE);
        }

        // WEEK_VIEW fade effect
        if (newState == ViewState.WEEK_VIEW) {
            for (int i = 0; i < gridFullMonthDays.getChildCount(); i++) {
                View child = gridFullMonthDays.getChildAt(i);
                Object tag = child.getTag();
                if (tag instanceof int[]) {
                    int row = ((int[]) tag)[0];
                    if (row == targetRowIndex) {
                        child.animate().alpha(1f).setDuration(250).withStartAction(() -> child.setVisibility(View.VISIBLE)).start();
                    } else {
                        child.animate().alpha(0f).setDuration(250).withEndAction(() -> child.setVisibility(View.GONE)).start();
                    }
                }
            }
        } else {
            for (int i = 0; i < gridFullMonthDays.getChildCount(); i++) {
                View child = gridFullMonthDays.getChildAt(i);
                child.setVisibility(View.VISIBLE);
                child.setAlpha(0f);
                child.animate().alpha(1f).setDuration(250).start();
            }
        }
        // Giữ highlight ngày đang chọn khi thay đổi view
        if (lastSelectedDayView != null) {
            highlightSelectedDay(lastSelectedDayView);
        }
    }

    private int findRowForDay(int day) {
        String dayStr = String.valueOf(day);
        for (int i = 0; i < gridFullMonthDays.getChildCount(); i++) {
            View child = gridFullMonthDays.getChildAt(i);
            if (child instanceof TextView) {
                TextView tv = (TextView) child;
                if (tv.getText() != null && dayStr.equals(tv.getText().toString().trim())) {
                    Object tag = tv.getTag();
                    if (tag instanceof int[]) {
                        return ((int[]) tag)[0]; // row
                    }
                }
            }
        }
        return -1;
    }

    private void displayCalendar() {
        String monthName = getMonthName(currentMonthIndex);
        tvMonthHeader.setText(String.format(Locale.getDefault(), "%s %d", monthName, currentYear));

        gridFullMonthDays.removeAllViews();

        Calendar calendar = Calendar.getInstance();
        calendar.set(currentYear, currentMonthIndex, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        // Get today's info to highlight
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK); // 1=Sunday
        int dayOffset = firstDayOfWeek - 1;
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        final int[] cellIndex = {0};

        int totalCellsNeeded = dayOffset + daysInMonth;
        actualRowsNeeded = (int) Math.ceil((double) totalCellsNeeded / DAYS_IN_WEEK);



        new Thread(() -> {
            List<EventEntity> events = db.eventDao().getEventsByMonth(
                    String.valueOf(currentYear),
                    String.format(Locale.getDefault(), "%02d", currentMonthIndex + 1)
            );

            Set<Integer> daysWithEvents = new HashSet<>();
            for (EventEntity e : events) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(e.getStartTime());
                daysWithEvents.add(cal.get(Calendar.DAY_OF_MONTH));
            }

            requireActivity().runOnUiThread(() -> {
                // DRAW EMPTY CELLS AT THE BEGINNING OF THE MONTH
                for (int i = 0; i < dayOffset; i++) {
                    gridFullMonthDays.addView(createMonthDayTextView("", false, cellIndex[0]++));
                }

                // DRAW DAYS OF THE MONTH
                for (int day = 1; day <= daysInMonth; day++) {
                    int cellIndexCurrent = cellIndex[0]++;
                    TextView tvDay = createMonthDayTextView(String.valueOf(day), true, cellIndexCurrent);

                    // highlight ngày được chọn
                    if (day == selectedDay.get(Calendar.DAY_OF_MONTH) &&
                            currentMonthIndex == selectedDay.get(Calendar.MONTH) &&
                            currentYear == selectedDay.get(Calendar.YEAR)) {
                        highlightSelectedDay(tvDay);
                        lastSelectedDayView = tvDay;
                        selectedDayOfMonth = day;
                    }
                    // thêm chấm nhỏ nếu ngày có sự kiện
                    if (daysWithEvents.contains(day)) {
                        tvDay.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.ic_dot);
                    }
                    gridFullMonthDays.addView(tvDay);
                }

                // FILL EMPTY CELLS AT THE END
                int cellsToFill = actualRowsNeeded * DAYS_IN_WEEK;
                for (int i = cellIndex[0]; i < cellsToFill; i++) {
                    gridFullMonthDays.addView(createMonthDayTextView("", false, cellIndex[0]++));
                }

                if (getView() != null) {
                    updateViewState(currentState, false);
                }
            });
        }).start();
    }

    //Hàm load sự kiện
    @SuppressLint("NotifyDataSetChanged")
    private void loadEventsForSelectedDay(int dayOfMonth, int monthIndex, int year) {
        //Cập nhật ngày được chọn
        selectedDay.set(year, monthIndex, dayOfMonth);
        selectedDay.set(Calendar.HOUR_OF_DAY, 0);
        selectedDay.set(Calendar.MINUTE, 0);
        selectedDay.set(Calendar.SECOND, 0);
        selectedDay.set(Calendar.MILLISECOND, 0);

        long dayStartMillis = selectedDay.getTimeInMillis();

        // query trong Thread Pool
        Executors.newSingleThreadExecutor().execute(() -> {
            List<EventEntity> loadedEvents = db.eventDao().getEventsByDay(dayStartMillis);

            // Cập nhật UI trên Main Thread
            if (getView() != null) {
                getView().post(() -> {
                    eventList.clear();
                    eventList.addAll(loadedEvents);
                    if (eventAdapter != null) {
                        eventAdapter.notifyDataSetChanged();
                    }
                });
            }
        });
    }

    private TextView createMonthDayTextView(String text, boolean isActualDay, int cellIndex) {
        TextView tv = new TextView(requireContext());

        int row = cellIndex / DAYS_IN_WEEK;
        int column = cellIndex % DAYS_IN_WEEK;

        tv.setTag(new int[]{row, column});

        float rowWeight = 1.0f / actualRowsNeeded;

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.columnSpec = GridLayout.spec(column,1,  1f);
        params.width = 0;
        params.rowSpec = GridLayout.spec(row, rowWeight);
        params.height = 0;
        tv.setLayoutParams(params);

        tv.setText(text);
        tv.setTypeface(Typeface.SANS_SERIF);
        tv.setTextSize(14f);
        tv.setGravity(Gravity.CENTER);

        //Lấy ngày hiện tại
        Calendar today = Calendar.getInstance();
        int todayDay = today.get(Calendar.DAY_OF_MONTH);
        int todayMonth = today.get(Calendar.MONTH);
        int todayYear = today.get(Calendar.YEAR);

        // Check xem tháng đang vẽ có phải tháng hiện tại không
        boolean isToday = false;
        if (isActualDay) {
            try {
                int day = Integer.parseInt(text);
                isToday = (day == todayDay) &&
                        (currentMonthIndex == todayMonth) &&
                        (currentYear == todayYear);
            } catch (NumberFormatException e) { }
        }

        // Áp style mặc định (dựa trên cột/chẵn lẻ) hoặc ẩn text nếu ô trống
        applyDefaultDayStyle(tv, isActualDay, isToday, column);

        if (isActualDay) {
            tv.setOnClickListener(v -> {
                int day = Integer.parseInt(text);

                if (lastSelectedDayView != null && lastSelectedDayView != v) {
                    resetDayStyle(lastSelectedDayView);
                }

                highlightSelectedDay(tv);

                lastSelectedDayView = tv;
                selectedDayOfMonth = day;

                // Query events for selecting day
                loadEventsForSelectedDay(day, currentMonthIndex, currentYear);
            });
        }
        return tv;
    }

    // Áp style mặc định (dựa trên cột) hoặc nếu là today thì dùng background today
    private void applyDefaultDayStyle(TextView tv, boolean isActualDay, boolean isToday, int column) {
        if (!isActualDay) {
            tv.setText(""); // ô trống
            tv.setTextColor(Color.TRANSPARENT);
            tv.setBackground(null);
            return;
        }

        if (isToday) {
            tv.setBackgroundResource(R.drawable.bg_current_day_for_month); // giữ drawable hiện tại cho "today"
            tv.setTextColor(Color.WHITE);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
            return;
        }

        if (column % 2 == 0) {
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.dard_grey));
            tv.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
        } else {
            tv.setTextColor(Color.BLACK);
            tv.setTypeface(Typeface.SANS_SERIF);
        }
        tv.setBackground(null);
    }

    private void resetDayStyle(TextView tv) {
        Object t = tv.getTag();
        int column = 0;

        if (t instanceof int[]) {
            column = ((int[]) t)[1];
        }

        int day = 0;
        try {
            day = Integer.parseInt(tv.getText().toString());
        } catch (NumberFormatException e) {}

        Calendar today = Calendar.getInstance();
        boolean isToday = (day == today.get(Calendar.DAY_OF_MONTH)) &&
                (currentMonthIndex == today.get(Calendar.MONTH)) &&
                (currentYear == today.get(Calendar.YEAR));

        if (isToday && day != selectedDay.get(Calendar.DAY_OF_MONTH)) {
            tv.setBackgroundResource(R.drawable.bg_current_day_for_month);
            tv.setTextColor(Color.WHITE);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            applyDefaultDayStyle(tv, true, false, column); // màu xám theo cột
        }
    }

    private void highlightSelectedDay(TextView tv) {
        Calendar today = Calendar.getInstance();
        int todayDay = today.get(Calendar.DAY_OF_MONTH);
        int todayMonth = today.get(Calendar.MONTH);
        int todayYear = today.get(Calendar.YEAR);

        // Lấy ngày của TextView đang chọn
        int day = Integer.parseInt(tv.getText().toString());

        if (day == todayDay && currentMonthIndex == todayMonth && currentYear == todayYear) {
            // Nếu ngày được chọn là hôm nay → xanh
            tv.setBackgroundResource(R.drawable.bg_current_day_for_month);
            tv.setTextColor(Color.WHITE);
            tv.setTypeface(Typeface.DEFAULT_BOLD);

        } else if(day != todayDay && currentMonthIndex == todayMonth && currentYear == todayYear){
            tv.setBackgroundResource(R.drawable.bg_select_day_for_month);
            tv.setTextColor(Color.BLACK);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
            if (currentMonthIndex == todayMonth && currentYear == todayYear) {
                // tìm đúng ngày hôm nay trong grid và đổi text thành xanh
                for (int i = 0; i < gridFullMonthDays.getChildCount(); i++) {
                    View v = gridFullMonthDays.getChildAt(i);
                    if (v instanceof TextView) {
                        TextView tvDay = (TextView) v;
                        try {
                            int d = Integer.parseInt(tvDay.getText().toString());
                            if (d == todayDay) {
                                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.bright_blue));
                                tvDay.setTypeface(Typeface.DEFAULT_BOLD);
                                break;
                            }
                        } catch (NumberFormatException e) {
                            // bỏ qua ô trống
                        }
                    }
                }
            }
        } else{
            // Nếu ngày được chọn không phải hôm nay → xám
            tv.setBackgroundResource(R.drawable.bg_select_day_for_month);
            tv.setTextColor(Color.BLACK);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
        }
    }

    private String getMonthName(int monthIndex) {
        Calendar tempCal = Calendar.getInstance();
        tempCal.set(Calendar.MONTH, monthIndex);
        return tempCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
    }
    
    /**
     * Refresh events after a new event is created
     */
    private void refreshEvents() {
        // Refresh calendar display and any event indicators
        displayCalendar();
        
        // Reload events for the selected day if needed
        if (selectedDay != null) {
            // Add any event loading logic here if MonthViewFragment shows events
        }
        
        // Notify other fragments about the event change
        Bundle result = new Bundle();
        result.putString("message", "Events refreshed");
        getParentFragmentManager().setFragmentResult("event_created", result);
    }
}