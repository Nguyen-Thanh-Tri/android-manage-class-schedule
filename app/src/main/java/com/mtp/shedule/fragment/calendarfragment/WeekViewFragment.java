package com.mtp.shedule.fragment.calendarfragment;

import static com.mtp.shedule.SelectColorDialog.COLOR_MAPPING_DRAWABLE;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.gridlayout.widget.GridLayout;

import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mtp.shedule.AddEventActivity;
import com.mtp.shedule.R;
import com.mtp.shedule.database.ConnDatabase;

import com.mtp.shedule.entity.EventEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class WeekViewFragment extends Fragment {
    // Các biến hằng số
    private static final float EVENT_SIDE_MARGIN_DP = 1f;
    private static final int DAYS_IN_WEEK = 7;
    private final Calendar currentWeekStart = Calendar.getInstance();
    private GridLayout gridWeekDays;
    private LinearLayout timeAxisContainer;
    private ConstraintLayout eventDrawingArea;
    private TextView tvMonthHeader;
    // Hằng số cho chiều cao một giờ (ví dụ: 70dp)
    private static final int HOURS_IN_DAY = 24;
    private static final float HOUR_HEIGHT_DP = 70;
    private static final int TIME_AXIS_WIDTH_DP = 35;
    private static final int WEEK_HEADER_SPACER = 28;
    TextView lastSelectedDayView = null;
    FloatingActionButton fabAddEvent;
    int selectedDayOfMonth = -1;
    ConnDatabase db;
    private View currentTimeLine;
    private final android.os.Handler timelineHandler = new android.os.Handler();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_week_view, container, false);

        db = ConnDatabase.getInstance(getContext());

        gridWeekDays = view.findViewById(R.id.gridWeekDays);
        timeAxisContainer = view.findViewById(R.id.time_axis);
        eventDrawingArea = view.findViewById(R.id.eventDrawingArea);
        tvMonthHeader = view.findViewById(R.id.tvMonthHeader);
        fabAddEvent = view.findViewById(R.id.fabAddEvent);

        // Set up fragment result listener for event updates
        setupFragmentResultListeners();
        //add event
        fabAddEvent.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddEventActivity.class);
            startActivity(intent);
        });

        findWeekStart();
        updateHeaderTitle();
        displayWeek();

        // Click vào header để mở DatePicker
        tvMonthHeader.setOnClickListener(v -> showDatePicker());

        view.post(() ->{
            drawTimeAxisLabels();
            initCurrentTimeLine();
            loadInitialEvents();
            timelineHandler.post(timelineRunnable);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh events when fragment becomes visible again
        refreshEvents();
        timelineHandler.post(timelineRunnable);
    }
    @Override
    public void onPause() {
        super.onPause();
        timelineHandler.removeCallbacks(timelineRunnable); // Dừng cập nhật khi không xem app
    }

    private void setupFragmentResultListeners() {
        // Listen for event creation/update results
        getParentFragmentManager().setFragmentResultListener("event_created", this, (requestKey, result) -> {
            refreshEvents();
        });
        
        getParentFragmentManager().setFragmentResultListener("event_updated", this, (requestKey, result) -> {
            refreshEvents();
        });
        
        getParentFragmentManager().setFragmentResultListener("event_deleted", this, (requestKey, result) -> {
            refreshEvents();
        });
    }

    /**
     * Public method to refresh events - can be called from parent activities/fragments
     */
    public void refreshEvents() {
        if (isAdded() && eventDrawingArea != null) {
            loadInitialEvents();
        }
    }
    private void loadInitialEvents() {
        Calendar weekStart = (Calendar) currentWeekStart.clone();
        Calendar weekEnd = (Calendar) currentWeekStart.clone();
        weekEnd.add(Calendar.DAY_OF_MONTH, DAYS_IN_WEEK);

        long startMillis = weekStart.getTimeInMillis();
        long endMillis = weekEnd.getTimeInMillis();

        new Thread(() -> {
            // Load regular events (excluding repeating events)
            List<EventEntity> events = db.eventDao().getEventsByWeek(startMillis, endMillis);

            // Filter out original repeating events (we'll show generated instances instead)
            List<EventEntity> filteredEvents = new ArrayList<>();
            for (EventEntity event : events) {
                if (event.getRepeatType() == null || "none".equals(event.getRepeatType())) {
                    filteredEvents.add(event);
                }
                // Skip original repeating events - we'll generate instances instead
            }

            // Load all courses and convert to weekly recurring events
            try {
                // Load all repeating events and generate instances for this week
                List<EventEntity> repeatingEvents = db.eventDao().getAllRepeatingEvents();
                if (repeatingEvents != null && !repeatingEvents.isEmpty()) {
                    List<EventEntity> generatedEvents = generateRepeatingEventInstances(repeatingEvents, weekStart);
                    filteredEvents.addAll(generatedEvents);
                }
            } catch (Exception e) {
                // Handle course loading error gracefully
                e.printStackTrace();
            }

            java.util.Map<Integer, List<EventEntity>> eventsByColumn = new java.util.HashMap<>();

            // Phân bổ sự kiện vào từng cột
            for (EventEntity e : filteredEvents) {
                Calendar c = Calendar.getInstance();
                c.setTimeInMillis(e.startTime);

                long dayStartOfEvent = toMillis(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
                int diffDays = (int) ((dayStartOfEvent - startMillis) / (24 * 60 * 60 * 1000L));

                if (diffDays >= 0 && diffDays < DAYS_IN_WEEK) {
                    eventsByColumn.computeIfAbsent(diffDays, k -> new java.util.ArrayList<>()).add(e);
                }
            }

            if (!isAdded()) {
                return;
            }

            requireActivity().runOnUiThread(() -> {
                removeOldEvents();
                for (java.util.Map.Entry<Integer, List<EventEntity>> entry : eventsByColumn.entrySet()) {
                    drawEventsForDay(entry.getValue(), entry.getKey());
                }
            });
        }).start();
    }

    private void showDatePicker() {
        Calendar calendar = (Calendar) currentWeekStart.clone();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // Set ngày được chọn làm ngày bắt đầu tuần
                    currentWeekStart.set(selectedYear, selectedMonth, selectedDay);

                    findWeekStart();

                    lastSelectedDayView = null;
                    selectedDayOfMonth = -1;

                    gridWeekDays.removeAllViews();
                    timeAxisContainer.removeAllViews();
                    eventDrawingArea.removeAllViews();

                    updateHeaderTitle();
                    displayWeek();

                    // Vẽ lại time axis và events nếu cần
                    eventDrawingArea.post(() -> {
                        drawTimeAxisLabels();
                        loadInitialEvents();
                    });
                },
                year,
                month,
                day
        );

        datePickerDialog.show();
    }
    private void updateHeaderTitle() {
        Calendar startOfWeek = (Calendar) currentWeekStart.clone();
        Calendar endOfWeek = (Calendar) currentWeekStart.clone();
        endOfWeek.add(Calendar.DAY_OF_MONTH, 6);

        SimpleDateFormat monthFormat = new SimpleDateFormat("MMM", Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("d", Locale.getDefault());
        SimpleDateFormat yearFormat = new SimpleDateFormat("yyyy", Locale.getDefault());

        String startMonth = monthFormat.format(startOfWeek.getTime());
        String endMonth = monthFormat.format(endOfWeek.getTime());
        String startDay = dayFormat.format(startOfWeek.getTime());
        String endDay = dayFormat.format(endOfWeek.getTime());
        String year = yearFormat.format(startOfWeek.getTime());

        String title;
        if (startOfWeek.get(Calendar.MONTH) == endOfWeek.get(Calendar.MONTH)) {
            title = String.format(Locale.getDefault(), "%s %s - %s, %s",
                    startMonth.toUpperCase(), startDay, endDay, year);
        } else if (startOfWeek.get(Calendar.YEAR) == endOfWeek.get(Calendar.YEAR)) {
            title = String.format(Locale.getDefault(), "%s %s - %s %s, %s",
                    startMonth.toUpperCase(), startDay, endMonth.toUpperCase(), endDay, year);
        } else {
            String endYear = yearFormat.format(endOfWeek.getTime());
            title = String.format(Locale.getDefault(), "%s %s, %s - %s %s, %s",
                    startMonth.toUpperCase(), startDay, year,
                    endMonth.toUpperCase(), endDay, endYear);
        }

        tvMonthHeader.setText(title);
    }

    /**
     * Generate instances of repeating events for the current week
     */
    private List<EventEntity> generateRepeatingEventInstances(List<EventEntity> repeatingEvents, Calendar weekStart) {
        List<EventEntity> generatedEvents = new ArrayList<>();

        for (EventEntity repeatingEvent : repeatingEvents) {
            if ("weekly".equals(repeatingEvent.getRepeatType())) {
                EventEntity weekInstance = generateWeeklyEventInstance(repeatingEvent, weekStart);
                if (weekInstance != null) {
                    generatedEvents.add(weekInstance);
                }
            }
            // Add other repeat types here (daily, monthly) as needed
        }

        return generatedEvents;
    }

    /**
     * Generate a weekly event instance for the current week
     */
    private EventEntity generateWeeklyEventInstance(EventEntity repeatingEvent, Calendar weekStart) {
        // Find the day in current week that matches the event's day
        int targetDayOfWeek = parseDayOfWeek(repeatingEvent.getDayOfWeek());
        if (targetDayOfWeek == -1) return null; // Invalid day

        Calendar eventDay = (Calendar) weekStart.clone();

        // Find the correct day in the week
        while (eventDay.get(Calendar.DAY_OF_WEEK) != targetDayOfWeek) {
            eventDay.add(Calendar.DAY_OF_MONTH, 1);
            // If we've gone past the week, this day doesn't exist in current week
            if (eventDay.getTimeInMillis() >= weekStart.getTimeInMillis() + (7 * 24 * 60 * 60 * 1000L)) {
                return null;
            }
        }

        // Only create instance if the day exists in current week
        if (eventDay.get(Calendar.DAY_OF_WEEK) == targetDayOfWeek &&
            eventDay.getTimeInMillis() < weekStart.getTimeInMillis() + (7 * 24 * 60 * 60 * 1000L)) {

            // Calculate the time difference to maintain the same time of day
            Calendar originalEventTime = Calendar.getInstance();
            originalEventTime.setTimeInMillis(repeatingEvent.getStartTime());

            eventDay.set(Calendar.HOUR_OF_DAY, originalEventTime.get(Calendar.HOUR_OF_DAY));
            eventDay.set(Calendar.MINUTE, originalEventTime.get(Calendar.MINUTE));
            eventDay.set(Calendar.SECOND, originalEventTime.get(Calendar.SECOND));
            eventDay.set(Calendar.MILLISECOND, originalEventTime.get(Calendar.MILLISECOND));

            long duration = repeatingEvent.getEndTime() - repeatingEvent.getStartTime();

            // Create instance for this week
            EventEntity instance = new EventEntity(
                repeatingEvent.getTitle(),
                repeatingEvent.getDescription(),
                eventDay.getTimeInMillis(),
                eventDay.getTimeInMillis() + duration
            );

            // Copy all properties from the repeating event
            instance.setColor(repeatingEvent.getColor());
            instance.setTeacher(repeatingEvent.getTeacher());
            instance.setRoom(repeatingEvent.getRoom());
            instance.setIsCourse(repeatingEvent.isCourse());
            instance.setId(-repeatingEvent.getId()); // Negative ID to distinguish instances

            return instance;
        }

        return null;
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



    private void drawTimeAxisLabels() {
        timeAxisContainer.removeAllViews();
        //padding đầu vào cuối lưới
        int topPaddingPx = (int) (10 * getResources().getDisplayMetrics().density);
        int bottomPaddingPx = (int) (10 * getResources().getDisplayMetrics().density);
        timeAxisContainer.setPadding(
                timeAxisContainer.getPaddingLeft(),
                topPaddingPx,
                timeAxisContainer.getPaddingRight(),
                bottomPaddingPx
        );
        eventDrawingArea.setPadding(
                eventDrawingArea.getPaddingLeft(),
                topPaddingPx,
                eventDrawingArea.getPaddingRight(),
                bottomPaddingPx
        );
        //

        float density = getResources().getDisplayMetrics().density;
        int hourHeightPx = (int) (HOUR_HEIGHT_DP * density);
        View lineZero = new View(requireContext());
        lineZero.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.very_light));
        lineZero.setTag("grid_line_0");

        ConstraintLayout.LayoutParams lpZero = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                (int) (1 * density)
        );
        lpZero.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        lpZero.topMargin = 0;
        lpZero.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        lpZero.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;

        eventDrawingArea.addView(lineZero, lpZero);
        int timeAxisWidthPx = (int) (TIME_AXIS_WIDTH_DP * density);
        // vẽ nhãn giờ
        for (int hour = 0; hour <= HOURS_IN_DAY; hour++) {
            TextView tvTime = new TextView(requireContext());

            String timeText = String.format(Locale.getDefault(), "%02d:00", hour);
            tvTime.setText(timeText);
            tvTime.setTextSize(8f);
            tvTime.setTextColor(ContextCompat.getColor(requireContext(), R.color.dard_grey));
            tvTime.setGravity(Gravity.CENTER_HORIZONTAL);
            tvTime.setPadding((int)(6 * density), 0, (int)(6 * density), 0);

            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    timeAxisWidthPx,
                    hourHeightPx
            );

            tvTime.setLayoutParams(params);
            timeAxisContainer.addView(tvTime);
        }
        // vẽ đường thẳng
        for (int hour = 0; hour <= HOURS_IN_DAY; hour++) {
            View line = new View(requireContext());
            line.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.very_light));

            line.setTag("grid_line_" + hour);

            ConstraintLayout.LayoutParams lineParams = new ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    (int) (1 * density)
            );
            lineParams.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            lineParams.topMargin = hour * hourHeightPx;
            lineParams.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            lineParams.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;

            ( eventDrawingArea).addView(line, lineParams);
        }

        ViewGroup.LayoutParams params = eventDrawingArea.getLayoutParams();
        params.height = (HOURS_IN_DAY) * hourHeightPx+90; //  0->24
        eventDrawingArea.setLayoutParams(params);
        // SỬA ĐỔI QUAN TRỌNG: Xóa Listener sau khi chạy
        eventDrawingArea.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Đảm bảo không gọi lại nhiều lần
                eventDrawingArea.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                drawDaySeparators();
            }
        });
    }
    private void drawDaySeparators() {
        eventDrawingArea.post(() -> {

            if (!isAdded()) {
                return;
            }

            int width = eventDrawingArea.getWidth();
            if (width == 0) return;

            float density = getResources().getDisplayMetrics().density;
            int rightPaddingPx = (int) (10 * density);

            float colWidth = (float) (width - rightPaddingPx) / DAYS_IN_WEEK;

            // Xóa đường cũ
            for (int i = eventDrawingArea.getChildCount() - 1; i >= 0; i--) {
                View v = eventDrawingArea.getChildAt(i);
                if (v.getTag() != null && v.getTag().toString().startsWith("day_separator_")) {
                    eventDrawingArea.removeView(v);
                }
            }
            // Vẽ 7 đường
            for (int i = 0; i <= DAYS_IN_WEEK; i++) {
                View line = new View(requireContext());
                line.setTag("day_separator_" + i);
                line.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.very_light));

                ConstraintLayout.LayoutParams p = new ConstraintLayout.LayoutParams(
                        (int) (1 * density),
                        ConstraintLayout.LayoutParams.MATCH_PARENT
                );

                p.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
                p.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
                p.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                line.setLayoutParams(p);
                line.setTranslationX(i * colWidth);
                eventDrawingArea.addView(line, p);
            }
        });
    }
    //Chèn khoảng trống vào đầu để đều với timeaxis
    private View createHeaderSpacer() {
        View spacer = new View(requireContext());

        // Tính pixel từ dp (phải khớp với TIME_AXIS_WIDTH_DP ở trên)
        float density = getResources().getDisplayMetrics().density;
        int widthPx = (int) (WEEK_HEADER_SPACER * density);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        // Cột 0, không có trọng số (weight) để giữ kích thước cố định
        params.columnSpec = GridLayout.spec(0);
        params.rowSpec = GridLayout.spec(0);
        params.width = widthPx;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;

        spacer.setLayoutParams(params);
        return spacer;
    }
    private void findWeekStart() {
        // Đặt ngày bắt đầu tuần là Chủ Nhật
        currentWeekStart.setFirstDayOfWeek(Calendar.SUNDAY);
        // Tìm ngày Chủ Nhật gần nhất
        while (currentWeekStart.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            currentWeekStart.add(Calendar.DAY_OF_MONTH, -1);
        }
        // Thiết lập về 00:00:00
        currentWeekStart.set(Calendar.HOUR_OF_DAY, 0);
        currentWeekStart.set(Calendar.MINUTE, 0);
        currentWeekStart.set(Calendar.SECOND, 0);
        currentWeekStart.set(Calendar.MILLISECOND, 0);

    }
    
    private void displayWeek() {
        gridWeekDays.removeAllViews();
        //8 cột, 1 cột rỗng + 7 ngày
        gridWeekDays.setColumnCount(8);
        gridWeekDays.addView(createHeaderSpacer());

        Calendar dayIterator = (Calendar) currentWeekStart.clone();

        for (int i = 0; i < DAYS_IN_WEEK; i++) {
            int dayOfMonth = dayIterator.get(Calendar.DAY_OF_MONTH);
            int monthIndex = dayIterator.get(Calendar.MONTH);
            int year = dayIterator.get(Calendar.YEAR);

            // i là cellIndex (0 đến 6)
            TextView tvDay = createWeekDayTextView(
                    String.valueOf(dayOfMonth),
                    true,
                    i+1,
                    dayOfMonth,
                    monthIndex,
                    year
            );

            gridWeekDays.addView(tvDay);

            // Tăng ngày lên 1 để lặp tiếp
            dayIterator.add(Calendar.DAY_OF_MONTH, 1);
        }
    }
    private void highlightSelectedDay(TextView tv) {
        Object tagObj = tv.getTag();
        if (!(tagObj instanceof int[])) {
            return;
        }

        int[] tagData = (int[]) tagObj;
        int dayOfMonth = tagData[1];
        int monthIndex = tagData[2];
        int year = tagData[3];

        Calendar today = Calendar.getInstance();
        int todayDay = today.get(Calendar.DAY_OF_MONTH);
        int todayMonth = today.get(Calendar.MONTH);
        int todayYear = today.get(Calendar.YEAR);

        // Kiểm tra xem có phải ngày hiện tại không
        boolean isToday = (dayOfMonth == todayDay) &&
                (monthIndex == todayMonth) &&
                (year == todayYear);

        if (isToday) {
            tv.setBackgroundResource(R.drawable.bg_current_day_for_month);
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            tv.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            tv.setBackgroundResource(R.drawable.bg_select_day_for_month);
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            tv.setTypeface(Typeface.DEFAULT_BOLD);
            // Nếu đang xem tháng/năm hiện tại, tìm và đổi text ngày hôm nay thành xanh
            if (monthIndex == todayMonth && year == todayYear) {
                for (int i = 0; i < gridWeekDays.getChildCount(); i++) {
                    View v = gridWeekDays.getChildAt(i);
                    if (v instanceof TextView) {
                        TextView tvDay = (TextView) v;
                        Object dayTag = tvDay.getTag();
                        if (dayTag instanceof int[]) {
                            int[] dayData = (int[]) dayTag;
                            int d = dayData[1]; // dayOfMonth
                            if (d == todayDay) {
                                tvDay.setTextColor(ContextCompat.getColor(requireContext(), R.color.bright_blue));
                                tvDay.setTypeface(Typeface.DEFAULT_BOLD);
                                tvDay.setBackground(null);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    private TextView createWeekDayTextView(String text, boolean isActualDay, int cellIndex,
                                           int dayOfMonth, int monthIndex, int year) {
        TextView tv = new TextView(requireContext());

        // Chỉ lưu column và các info cần thiết khác vào tag
        tv.setTag(new int[]{cellIndex, dayOfMonth, monthIndex, year});

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.columnSpec = GridLayout.spec(cellIndex, 1, 1f);
        params.rowSpec = GridLayout.spec(0);
        params.width = 0;
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        tv.setLayoutParams(params);

        tv.setText(text);
        tv.setTextSize(14f);
        tv.setGravity(Gravity.CENTER);

        boolean isToday = checkIsToday(dayOfMonth, monthIndex, year);
        applyDefaultDayStyle(tv, isActualDay, isToday, cellIndex);

        // Thêm OnClickListener để chọn ngày và load events cho ngày đó
        if (isActualDay) {
            tv.setOnClickListener(v -> {
                clearAllHighlight();
                highlightSelectedDay(tv);

                lastSelectedDayView = tv;
                selectedDayOfMonth = dayOfMonth;
            });
        }
        return tv;
    }

    private void drawEventsForDay(List<EventEntity> events, int columnIndex) {
        eventDrawingArea.post(() -> {
            if (!isAdded()) {
                return;
            }

            float density = getResources().getDisplayMetrics().density;
            float hourHeightPx = HOUR_HEIGHT_DP * density;

            int width = eventDrawingArea.getWidth();

            int rightPaddingPx = (int) (10 * density);
            float colWidth = (float) (width - rightPaddingPx) / DAYS_IN_WEEK;

            // Tính toán Margin 1dp mỗi bên
            float totalMarginPx = EVENT_SIDE_MARGIN_DP * 2 * density;
            float leftMarginPx = EVENT_SIDE_MARGIN_DP * density;

            float eventWidth = colWidth - totalMarginPx;

            Calendar c1 = Calendar.getInstance();
            Calendar c2 = Calendar.getInstance();

            for (EventEntity e : events) {
                // Lấy giờ bắt đầu
                c1.setTimeInMillis(e.startTime);
                int startHour = c1.get(Calendar.HOUR_OF_DAY);
                int startMinute = c1.get(Calendar.MINUTE);

                // Lấy giờ kết thúc
                c2.setTimeInMillis(e.endTime);
                int endHour = c2.get(Calendar.HOUR_OF_DAY);
                int endMinute = c2.get(Calendar.MINUTE);

                float start = (startHour + startMinute / 60f) * hourHeightPx;
                float end = (endHour + endMinute / 60f) * hourHeightPx;

                float height = end - start;
                if (height < 20 * density) height = 20 * density; // không event quá nhỏ


                // TẠO EVENT BOX
                TextView eventView = new TextView(requireContext());
                eventView.setText(e.title);
                eventView.setTextSize(10);
                eventView.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
                eventView.setGravity(Gravity.TOP | Gravity.START);
                eventView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                eventView.setPadding(12, 8, 12, 8);
                eventView.setTag("event_" + e.id);

                Drawable gradient = Objects.requireNonNull(ContextCompat.getDrawable(requireContext(), COLOR_MAPPING_DRAWABLE[e.color])).mutate();

                // Lấy layout shape (bo tròn, stroke…)
                GradientDrawable shape = (GradientDrawable) Objects.requireNonNull(ContextCompat.getDrawable(requireContext(), R.drawable.bg_event_shape)).mutate();

                // Ép màu từ gradient vào shape
                if (gradient instanceof GradientDrawable) {
                    int[] colors = ((GradientDrawable) gradient).getColors();
                    if (colors != null) {
                        shape.setColors(colors); // giữ nguyên gradient
                    }
                }

                // Gán vào view
                eventView.setBackground(shape);
                ConstraintLayout.LayoutParams lp =
                        new ConstraintLayout.LayoutParams(
                                (int) eventWidth,
                                (int) height
                        );

                lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;

                eventView.setLayoutParams(lp);

                // scroll, dịch theo
                eventView.setTranslationX(columnIndex * colWidth + leftMarginPx);
                eventView.setTranslationY(start);

                eventDrawingArea.addView(eventView);
            }
        });
    }

    private void removeOldEvents() {
        for (int i = eventDrawingArea.getChildCount() - 1; i >= 0; i--) {
            View v = eventDrawingArea.getChildAt(i);
            Object tag = v.getTag();
            if (tag != null && tag.toString().startsWith("event_")) {
                eventDrawingArea.removeView(v);
            }
        }
    }
    private void applyDefaultDayStyle(TextView tv, boolean isActualDay, boolean isToday, int column) {
        if (!isActualDay) {
            tv.setText(""); // ô trống
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.transparent));
            tv.setBackground(null);
            return;
        }

        if (isToday) {
            tv.setBackgroundResource(R.drawable.bg_current_day_for_month); // giữ drawable hiện tại cho "today"
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
            tv.setTypeface(Typeface.DEFAULT_BOLD);
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
            // [column, dayOfMonth, monthIndex, year]
            column = ((int[]) t)[0];
        }

        applyDefaultDayStyle(tv, true, false, column);

    }
    private void clearAllHighlight() {
        for (int i = 0; i < gridWeekDays.getChildCount(); i++) {
            View child = gridWeekDays.getChildAt(i);
            if (child instanceof TextView) {
                resetDayStyle((TextView) child);
            }
        }
    }

    private long toMillis(int year, int month, int day) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
    private boolean checkIsToday(int dayOfMonth, int monthIndex, int year) {
        Calendar today = Calendar.getInstance();
        return (dayOfMonth == today.get(Calendar.DAY_OF_MONTH)) &&
                (monthIndex == today.get(Calendar.MONTH)) &&
                (year == today.get(Calendar.YEAR));
    }

    private final Runnable timelineRunnable = new Runnable() {
        @Override
        public void run() {
            updateCurrentTimeLine();
            // Cập nhật lại mỗi 60 giây
            timelineHandler.postDelayed(this, 60000);
        }
    };
    private void initCurrentTimeLine() {
        if (currentTimeLine != null) eventDrawingArea.removeView(currentTimeLine);

        float density = getResources().getDisplayMetrics().density;

        // Tạo container cho line và circle
        currentTimeLine = new View(requireContext());
        currentTimeLine.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.azure)); // Màu xanh dương
        currentTimeLine.setTag("current_time_line");

        ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT, // Sẽ set width động theo cột
                (int) (1.5f * density) // Độ dày 2dp
        );
        lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;

        currentTimeLine.setLayoutParams(lp);
        currentTimeLine.setZ(10f);
        eventDrawingArea.addView(currentTimeLine);
    }
    private void updateCurrentTimeLine() {
        if (!isAdded() || eventDrawingArea == null || currentTimeLine == null) return;

        Calendar now = Calendar.getInstance();
        long nowMillis = now.getTimeInMillis();

        // Kiểm tra xem tuần hiện tại đang xem có chứa ngày "Hôm nay" không
        Calendar weekStart = (Calendar) currentWeekStart.clone();
        Calendar weekEnd = (Calendar) currentWeekStart.clone();
        weekEnd.add(Calendar.DAY_OF_MONTH, 7);

        if (nowMillis < weekStart.getTimeInMillis() || nowMillis >= weekEnd.getTimeInMillis()) {
            currentTimeLine.setVisibility(View.GONE);
            return;
        }

        currentTimeLine.setVisibility(View.VISIBLE);

        float density = getResources().getDisplayMetrics().density;
        float hourHeightPx = HOUR_HEIGHT_DP * density;

        // Vị trí Y (Giờ hiện tại)
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int minute = now.get(Calendar.MINUTE);
        float positionY = (hour + minute / 60f) * hourHeightPx;

        // Vị trí X của dấu chấm tròn (Cột ngày hiện tại)
        int width = eventDrawingArea.getWidth();
        int rightPaddingPx = (int) (10 * density);
        float colWidth = (float) (width - rightPaddingPx) / DAYS_IN_WEEK;

        // Tính xem hôm nay là ngày thứ mấy trong tuần đang hiển thị (0-6)
        long diff = now.get(Calendar.DAY_OF_YEAR) - currentWeekStart.get(Calendar.DAY_OF_YEAR);
        // Xử lý trường hợp lệch năm
        if (diff < 0) diff += now.getActualMaximum(Calendar.DAY_OF_YEAR);
        int dayIndex = (int) diff;

        // Cập nhật vị trí đường kẻ (Chỉ cần di chuyển Y)
        currentTimeLine.setTranslationY(positionY);
    }
}