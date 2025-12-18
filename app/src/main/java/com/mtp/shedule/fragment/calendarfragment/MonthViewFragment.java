package com.mtp.shedule.fragment.calendarfragment;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.content.Intent;
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
import java.util.Collections;
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
    // List hiển thị trên RecyclerView
    private final List<EventEntity> eventList = new ArrayList<>();
    // List CACHE chứa toàn bộ dữ liệu thô từ DB (LiveData trả về)
    private final List<EventEntity> allRawEventsCache = new ArrayList<>();
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
    FloatingActionButton fabAddEvent;
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
            //Dùng liveData
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

        // --- SETUP LIVEDATA OBSERVER ---
        db.eventDao().getAllEventsLiveData().observe(getViewLifecycleOwner(), events -> {
            // 1. Cập nhật Cache
            allRawEventsCache.clear();
            if (events != null) {
                allRawEventsCache.addAll(events);
            }

            // 2. Vẽ lại lịch (cập nhật hanlde, light event)
            displayCalendar();

            // 3. Load lại list sự kiện cho ngày đang chọn
            processEventsForSelectedDay();
        });

        // Click Item -> View Mode
        eventAdapter.setOnItemClickListener(event ->{
            Intent intent = new Intent(getContext(), AddEventActivity.class);
            intent.putExtra("event_item", event);
            intent.putExtra("is_view_mode", true);
            addEventLauncher.launch(intent);
        });

        // Click FAB -> Add Mode
        fabAddEvent.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddEventActivity.class);
            intent.putExtra("is_view_mode", false);
            addEventLauncher.launch(intent);
        });

        // Thiết lập trạng thái ban đầu dựa trên currentState
        view.post(() -> updateViewState(currentState, false));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        displayCalendar();
        processEventsForSelectedDay();
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
        boolean isCurrentMonth = (newMonthIndex == now.get(Calendar.MONTH) && year == now.get(Calendar.YEAR));
        int newSelectedDay = isCurrentMonth ? now.get(Calendar.DAY_OF_MONTH) : 1;

        // Cập nhật selectedDay HOÀN CHỈNH
        selectedDay.set(year, newMonthIndex, newSelectedDay);
        selectedDay.set(Calendar.HOUR_OF_DAY, 0);
        selectedDay.set(Calendar.MINUTE, 0);
        selectedDay.set(Calendar.SECOND, 0);
        selectedDay.set(Calendar.MILLISECOND, 0);

        if (getView() != null) {
            gridFullMonthDays.removeAllViews();
            displayCalendar();
            processEventsForSelectedDay(); // Gọi hàm xử lý từ cache

            // Cập nhật view state sau khi vẽ xong
            gridFullMonthDays.post(() -> {
                updateViewState(currentState, false);
            });
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

    @SuppressLint("NotifyDataSetChanged")
    private void processEventsForSelectedDay() {
        // Lấy thời gian bắt đầu và kết thúc của ngày đang chọn
        long dayStartMillis = selectedDay.getTimeInMillis();
        long dayEndMillis = dayStartMillis + (24 * 60 * 60 * 1000L) - 1;

        List<EventEntity> filteredEvents = new ArrayList<>();

        // 1. Lọc sự kiện thường (One-time)
        for (EventEntity event : allRawEventsCache) {
            if (event.getRepeatType() == null || "none".equals(event.getRepeatType())) {
                // Kiểm tra xem event có nằm trong ngày đang chọn không
                if (event.getStartTime() >= dayStartMillis && event.getStartTime() <= dayEndMillis) {
                    filteredEvents.add(event);
                }
            }
        }

        // 2. Sinh sự kiện lặp lại (Repeating)
        for (EventEntity repeatingEvent : allRawEventsCache) {
            if ("weekly".equals(repeatingEvent.getRepeatType())) {
                EventEntity dayInstance = generateWeeklyEventInstanceForSpecificDay(repeatingEvent, selectedDay);
                if (dayInstance != null) {
                    filteredEvents.add(dayInstance);
                }
            }
        }

        // 3. Sắp xếp (Start -> End -> Title)
        Collections.sort(filteredEvents, (e1, e2) -> {
            int startCompare = Long.compare(e1.getStartTime(), e2.getStartTime());
            if (startCompare != 0) return startCompare;

            int endCompare = Long.compare(e1.getEndTime(), e2.getEndTime());
            if (endCompare != 0) return endCompare;

            String title1 = e1.getTitle() == null ? "" : e1.getTitle();
            String title2 = e2.getTitle() == null ? "" : e2.getTitle();
            return title1.compareToIgnoreCase(title2);
        });

        // 4. Cập nhật Adapter
        eventList.clear();
        eventList.addAll(filteredEvents);
        eventAdapter.notifyDataSetChanged();
    }

    private void displayCalendar() {
        String monthName = getMonthName(currentMonthIndex);
        tvMonthHeader.setText(String.format(Locale.getDefault(), "%s %d", monthName, currentYear));

        gridFullMonthDays.removeAllViews();

        Calendar calendar = Calendar.getInstance();
        calendar.set(currentYear, currentMonthIndex, 1);

        // Get today's info to highlight
        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK); // 1=Sunday
        int dayOffset = firstDayOfWeek - 1;
        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);
        final int[] cellIndex = {0};

        int totalCellsNeeded = dayOffset + daysInMonth;
        actualRowsNeeded = (int) Math.ceil((double) totalCellsNeeded / DAYS_IN_WEEK);

        // --- TÍNH TOÁN CÁC NGÀY CÓ SỰ KIỆN (Dựa trên Cache) ---
        Set<Integer> daysWithEvents = new HashSet<>();

        // 1. Check sự kiện thường
        for (EventEntity e : allRawEventsCache) {
            if (e.getRepeatType() == null || "none".equals(e.getRepeatType())) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(e.getStartTime());
                if (cal.get(Calendar.MONTH) == currentMonthIndex && cal.get(Calendar.YEAR) == currentYear) {
                    daysWithEvents.add(cal.get(Calendar.DAY_OF_MONTH));
                }
            }
        }

        // 2. Check sự kiện lặp lại
        List<EventEntity> repeatingEvents = new ArrayList<>();
        for(EventEntity e : allRawEventsCache) {
            if (e.getRepeatType() != null && !e.getRepeatType().equals("none")) {
                repeatingEvents.add(e);
            }
        }
        if (!repeatingEvents.isEmpty()) {
            List<EventEntity> generatedEvents = generateRepeatingEventInstancesForMonth(repeatingEvents);
            for (EventEntity e : generatedEvents) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(e.getStartTime());
                // Vì generateRepeatingEventInstancesForMonth đã lọc theo tháng hiện tại rồi
                daysWithEvents.add(cal.get(Calendar.DAY_OF_MONTH));
            }
        }

        // --- VẼ LỊCH ---
        // Empty start cells
        for (int i = 0; i < dayOffset; i++) {
            gridFullMonthDays.addView(createMonthDayTextView("", false, cellIndex[0]++));
        }

        // Days
        for (int day = 1; day <= daysInMonth; day++) {
            int cellIndexCurrent = cellIndex[0]++;
            TextView tvDay = createMonthDayTextView(String.valueOf(day), true, cellIndexCurrent);

            // Highlight Logic
            if (day == selectedDay.get(Calendar.DAY_OF_MONTH) &&
                    currentMonthIndex == selectedDay.get(Calendar.MONTH) &&
                    currentYear == selectedDay.get(Calendar.YEAR)) {
                highlightSelectedDay(tvDay);
                lastSelectedDayView = tvDay;
                selectedDayOfMonth = day;
            }
            // Dot indicator
            if (daysWithEvents.contains(day)) {
                tvDay.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, R.drawable.ic_dot);
            }
            gridFullMonthDays.addView(tvDay);
        }

        // Empty end cells
        int cellsToFill = actualRowsNeeded * DAYS_IN_WEEK;
        for (int i = cellIndex[0]; i < cellsToFill; i++) {
            gridFullMonthDays.addView(createMonthDayTextView("", false, cellIndex[0]++));
        }

        if (getView() != null) {
            updateViewState(currentState, false);
        }
    }

    /**
     * Generate a weekly event instance for a specific day
     */
    private EventEntity generateWeeklyEventInstanceForSpecificDay(EventEntity repeatingEvent, Calendar targetDay) {
        // Check if the target day matches the event's day of week
        int targetDayOfWeek = parseDayOfWeek(repeatingEvent.getDayOfWeek());
        if (targetDayOfWeek == -1 || targetDay.get(Calendar.DAY_OF_WEEK) != targetDayOfWeek) {
            return null; // This event doesn't occur on the target day
        }

        // Calculate the time for this instance
        Calendar eventTime = (Calendar) targetDay.clone();
        Calendar originalEventTime = Calendar.getInstance();
        originalEventTime.setTimeInMillis(repeatingEvent.getStartTime());

        eventTime.set(Calendar.HOUR_OF_DAY, originalEventTime.get(Calendar.HOUR_OF_DAY));
        eventTime.set(Calendar.MINUTE, originalEventTime.get(Calendar.MINUTE));
        eventTime.set(Calendar.SECOND, originalEventTime.get(Calendar.SECOND));
        eventTime.set(Calendar.MILLISECOND, originalEventTime.get(Calendar.MILLISECOND));

        long duration = repeatingEvent.getEndTime() - repeatingEvent.getStartTime();

        // Create instance for this day
        EventEntity instance = new EventEntity(
            repeatingEvent.getTitle(),
            repeatingEvent.getDescription(),
            eventTime.getTimeInMillis(),
            eventTime.getTimeInMillis() + duration
        );

        // Copy all properties from the repeating event
        instance.setColor(repeatingEvent.getColor());
        instance.setTeacher(repeatingEvent.getTeacher());
        instance.setRoom(repeatingEvent.getRoom());
        instance.setIsCourse(repeatingEvent.isCourse());
        instance.setId(-repeatingEvent.getId()); // Negative ID to distinguish instances

        return instance;
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

                // Cập nhật selectedDay
                selectedDay.set(currentYear, currentMonthIndex, day);
                selectedDay.set(Calendar.HOUR_OF_DAY, 0);
                selectedDay.set(Calendar.MINUTE, 0);
                selectedDay.set(Calendar.SECOND, 0);
                selectedDay.set(Calendar.MILLISECOND, 0);

                // Gọi xử lý từ Cache
                processEventsForSelectedDay();
            });
        }
        return tv;
    }

    // Áp style mặc định (dựa trên cột) hoặc nếu là today thì dùng background today
    private void applyDefaultDayStyle(TextView tv, boolean isActualDay, boolean isToday, int column) {
        if (!isActualDay) {
            tv.setText(""); // ô trống
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.transparent));
            tv.setBackground(null);
            return;
        }

        if (isToday) {
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.bright_blue));
            tv.setTypeface(Typeface.DEFAULT_BOLD);
            tv.setBackground(null);
            return;
        }

        if (column % 2 == 0) {
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.dard_grey));
            tv.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL);
        } else {
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
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
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            tv.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            applyDefaultDayStyle(tv, true, false, column); // màu xám theo cột
        }
    }

    private void highlightSelectedDay(TextView tv) {
        Calendar today = Calendar.getInstance();
        int day = Integer.parseInt(tv.getText().toString());
        boolean isToday = (day == today.get(Calendar.DAY_OF_MONTH) && currentMonthIndex == today.get(Calendar.MONTH) && currentYear == today.get(Calendar.YEAR));

        if (isToday) {
            tv.setBackgroundResource(R.drawable.bg_current_day_for_month); // Nền đặc
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            tv.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            tv.setBackgroundResource(R.drawable.bg_select_day_for_month); // Nền rỗng/xám
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            tv.setTypeface(Typeface.DEFAULT_BOLD);

            // Nếu có ngày "Hôm nay" đang hiện trên lưới, reset màu chữ cho nó (vì nó bị mất focus)
            if (currentMonthIndex == today.get(Calendar.MONTH) && currentYear == today.get(Calendar.YEAR)) {
                for (int i = 0; i < gridFullMonthDays.getChildCount(); i++) {
                    View v = gridFullMonthDays.getChildAt(i);
                    if (v instanceof TextView) {
                        TextView tvItem = (TextView) v;
                        try {
                            if (!tvItem.getText().toString().isEmpty()) {
                                int d = Integer.parseInt(tvItem.getText().toString());
                                if (d == today.get(Calendar.DAY_OF_MONTH) && d != day) {
                                    tvItem.setBackground(null);
                                    tvItem.setTextColor(ContextCompat.getColor(requireContext(), R.color.bright_blue));
                                    tvItem.setTypeface(Typeface.DEFAULT_BOLD);
                                }
                            }
                        } catch (NumberFormatException e) {}
                    }
                }
            }
        }
    }

    private String getMonthName(int monthIndex) {
        Calendar tempCal = Calendar.getInstance();
        tempCal.set(Calendar.MONTH, monthIndex);
        return tempCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
    }

    /**
     * Generate instances of repeating events for the current month
     */
    private List<EventEntity> generateRepeatingEventInstancesForMonth(List<EventEntity> repeatingEvents) {
        List<EventEntity> generatedEvents = new ArrayList<>();

        for (EventEntity repeatingEvent : repeatingEvents) {
            if ("weekly".equals(repeatingEvent.getRepeatType())) {
                List<EventEntity> weeklyInstances = generateWeeklyEventInstancesForMonth(repeatingEvent);
                generatedEvents.addAll(weeklyInstances);
            }
            // Add other repeat types here (daily, monthly) as needed
        }

        return generatedEvents;
    }

    /**
     * Generate weekly event instances for the current month
     */
    private List<EventEntity> generateWeeklyEventInstancesForMonth(EventEntity repeatingEvent) {
        List<EventEntity> instances = new ArrayList<>();

        // Get target day of week
        int targetDayOfWeek = parseDayOfWeek(repeatingEvent.getDayOfWeek());
        if (targetDayOfWeek == -1) return instances; // Invalid day

        // Start from the first day of the month
        Calendar monthStart = Calendar.getInstance();
        monthStart.set(currentYear, currentMonthIndex, 1, 0, 0, 0);
        monthStart.set(Calendar.MILLISECOND, 0);

        // End at the last day of the month
        Calendar monthEnd = Calendar.getInstance();
        monthEnd.set(currentYear, currentMonthIndex, monthStart.getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59);
        monthEnd.set(Calendar.MILLISECOND, 999);

        // Find all occurrences of the target day of week in this month
        Calendar currentDay = (Calendar) monthStart.clone();

        // Move to the first occurrence of the target day in the month
        while (currentDay.get(Calendar.DAY_OF_WEEK) != targetDayOfWeek &&
               currentDay.getTimeInMillis() <= monthEnd.getTimeInMillis()) {
            currentDay.add(Calendar.DAY_OF_MONTH, 1);
        }

        // Generate instances for each week in the month
        while (currentDay.getTimeInMillis() <= monthEnd.getTimeInMillis()) {
            // Calculate the time for this instance
            Calendar originalEventTime = Calendar.getInstance();
            originalEventTime.setTimeInMillis(repeatingEvent.getStartTime());

            currentDay.set(Calendar.HOUR_OF_DAY, originalEventTime.get(Calendar.HOUR_OF_DAY));
            currentDay.set(Calendar.MINUTE, originalEventTime.get(Calendar.MINUTE));
            currentDay.set(Calendar.SECOND, originalEventTime.get(Calendar.SECOND));
            currentDay.set(Calendar.MILLISECOND, originalEventTime.get(Calendar.MILLISECOND));

            long duration = repeatingEvent.getEndTime() - repeatingEvent.getStartTime();

            // Create instance for this occurrence
            EventEntity instance = new EventEntity(
                repeatingEvent.getTitle(),
                repeatingEvent.getDescription(),
                currentDay.getTimeInMillis(),
                currentDay.getTimeInMillis() + duration
            );

            // Copy all properties from the repeating event
            instance.setColor(repeatingEvent.getColor());
            instance.setTeacher(repeatingEvent.getTeacher());
            instance.setRoom(repeatingEvent.getRoom());
            instance.setIsCourse(repeatingEvent.isCourse());
            instance.setId(-repeatingEvent.getId()); // Negative ID to distinguish instances

            instances.add(instance);

            // Move to next week
            currentDay.add(Calendar.WEEK_OF_YEAR, 1);
        }

        return instances;
    }

    /**
     * Parse day of week string to Calendar constant
     */
    private int parseDayOfWeek(String dayOfWeek) {
        if (dayOfWeek == null) return -1;

        switch (dayOfWeek.toLowerCase().trim()) {
            case "sunday":
            case "chủ nhật":
            case "cn":
                return Calendar.SUNDAY;
            case "monday":
            case "thứ hai":
            case "t2":
                return Calendar.MONDAY;
            case "tuesday":
            case "thứ ba":
            case "t3":
                return Calendar.TUESDAY;
            case "wednesday":
            case "thứ tư":
            case "t4":
                return Calendar.WEDNESDAY;
            case "thursday":
            case "thứ năm":
            case "t5":
                return Calendar.THURSDAY;
            case "friday":
            case "thứ sáu":
            case "t6":
                return Calendar.FRIDAY;
            case "saturday":
            case "thứ bảy":
            case "t7":
                return Calendar.SATURDAY;
            default:
                return -1;
        }
    }
}