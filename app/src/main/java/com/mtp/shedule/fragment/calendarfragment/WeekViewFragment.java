package com.mtp.shedule.fragment.calendarfragment;

import static com.mtp.shedule.SelectColorDialog.COLOR_MAPPING_DRAWABLE;

import android.graphics.Color;
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

import com.mtp.shedule.R;
import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.EventEntity;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class WeekViewFragment extends Fragment {
    // Các biến hằng số
    int currentMonthIndex;
    int currentYear;
    private static final float EVENT_SIDE_MARGIN_DP = 1f;
    private static final int DAYS_IN_WEEK = 7;
    private final Calendar currentWeekStart = Calendar.getInstance();
    private GridLayout gridWeekDays;
    private LinearLayout timeAxisContainer;
    private ConstraintLayout eventDrawingArea;
    // Hằng số cho chiều cao một giờ (ví dụ: 70dp)
    private static final int HOURS_IN_DAY = 24;
    private static final float HOUR_HEIGHT_DP = 70;
    TextView lastSelectedDayView = null;
    int selectedDayOfMonth = -1;
     ConnDatabase db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_week_view, container, false);

        db = ConnDatabase.getInstance(getContext());

        gridWeekDays = view.findViewById(R.id.gridWeekDays);
        timeAxisContainer = view.findViewById(R.id.time_axis);
        eventDrawingArea = view.findViewById(R.id.eventDrawingArea);

        // Set up fragment result listener for event updates
        setupFragmentResultListeners();

        findWeekStart();
        displayWeek();

        view.post(() ->{
            drawTimeAxisLabels();
            loadInitialEvents();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh events when fragment becomes visible again
        refreshEvents();
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
            List<EventEntity> events = db.eventDao().getEventsByWeek(startMillis, endMillis);

            java.util.Map<Integer, List<EventEntity>> eventsByColumn = new java.util.HashMap<>();

                // Phân bổ sự kiện vào từng cột
                for (EventEntity e : events) {
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
                    ViewGroup.LayoutParams.WRAP_CONTENT,
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
    private void findWeekStart() {
        // Đặt ngày bắt đầu tuần là Chủ Nhật
        currentWeekStart.setFirstDayOfWeek(Calendar.SUNDAY);
        currentWeekStart.set(Calendar.DAY_OF_WEEK, currentWeekStart.getFirstDayOfWeek());
        // Thiết lập về 00:00:00
        currentWeekStart.set(Calendar.HOUR_OF_DAY, 0);
        currentWeekStart.set(Calendar.MINUTE, 0);
        currentWeekStart.set(Calendar.SECOND, 0);
        currentWeekStart.set(Calendar.MILLISECOND, 0);
        
        // Initialize currentMonthIndex and currentYear
        currentMonthIndex = currentWeekStart.get(Calendar.MONTH);
        currentYear = currentWeekStart.get(Calendar.YEAR);
    }
    
    private void displayWeek() {
        gridWeekDays.removeAllViews();
        Calendar dayIterator = (Calendar) currentWeekStart.clone();

        for (int i = 0; i < DAYS_IN_WEEK; i++) {
            int dayOfMonth = dayIterator.get(Calendar.DAY_OF_MONTH);
            int monthIndex = dayIterator.get(Calendar.MONTH);
            int year = dayIterator.get(Calendar.YEAR);

            // i là cellIndex (0 đến 6)
            TextView tvDay = createWeekDayTextView(
                    String.valueOf(dayOfMonth),
                    true,
                    i,
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
        int day = Integer.parseInt(tv.getText().toString());

        Calendar today = Calendar.getInstance();
        boolean isToday = (day == today.get(Calendar.DAY_OF_MONTH)) &&
                (currentMonthIndex == today.get(Calendar.MONTH)) &&
                (currentYear == today.get(Calendar.YEAR));

        tv.setTextColor(Color.WHITE);
        tv.setTypeface(Typeface.DEFAULT_BOLD);
        if (isToday) {
            tv.setBackgroundResource(R.drawable.bg_current_day_for_month);
        }else {
            tv.setBackgroundResource(R.drawable.bg_select_day_for_month);
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
                eventView.setTextColor(Color.WHITE);
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
        // Xóa event cũ
        for (int i = eventDrawingArea.getChildCount() - 1; i >= 0; i--) {
            View v = eventDrawingArea.getChildAt(i);
            if (v.getTag() != null && v.getTag().toString().startsWith("event_")) {
                eventDrawingArea.removeView(v);
            }
        }
    }
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
}