package com.mtp.shedule.fragment.calendarfragment;

import static com.mtp.shedule.SelectColorDialog.COLOR_MAPPING_DRAWABLE;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.gridlayout.widget.GridLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mtp.shedule.AddEventActivity;
import com.mtp.shedule.R;
import com.mtp.shedule.database.ConnDatabase;
import com.mtp.shedule.entity.EventEntity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.Executors;

public class DayViewFragment extends Fragment {

    private GridLayout gridWeekDays;
    private LinearLayout timeAxisContainer;
    private ConstraintLayout eventDrawingArea;
    private TextView tvMonthHeader;
    private final Calendar currentWeekStart = Calendar.getInstance();
    int selectedDayOfMonth = -1;
    private static final int DAYS_IN_WEEK = 7;
    TextView lastSelectedDayView = null;
    private static final int HOURS_IN_DAY = 24;
    private static final float HOUR_HEIGHT_DP = 70;
    private static final float EVENT_SIDE_MARGIN_DP = 1f;
    FloatingActionButton fabAddEvent;
    private ConnDatabase db;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_day_view, container, false);

        gridWeekDays = view.findViewById(R.id.gridWeekDays);
        timeAxisContainer = view.findViewById(R.id.time_axis);
        eventDrawingArea = view.findViewById(R.id.eventDrawingArea);
        tvMonthHeader = view.findViewById(R.id.tvMonthHeader);
        fabAddEvent = view.findViewById(R.id.fabAddEvent);

        db = ConnDatabase.getInstance(requireContext());

        findWeekStart();
        updateHeaderTitle();
        displayWeek();

        //add event
        fabAddEvent.setOnClickListener(v -> {
            Intent intent = new Intent(getContext(), AddEventActivity.class);
            startActivity(intent);
        });

        // Click vào header để mở DatePicker
        tvMonthHeader.setOnClickListener(v -> showDatePicker());

        view.post(() ->{
            drawTimeAxisLabels();
            loadEventsForSelectedDay();
        });

        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        // Reload events khi quay lại fragment
        loadEventsForSelectedDay();
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
                        loadEventsForSelectedDay();
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
        //vẽ line đầu tiên(00:00)
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

            float density = getResources().getDisplayMetrics().density;

            // Xóa đường cũ
            for (int i = eventDrawingArea.getChildCount() - 1; i >= 0; i--) {
                View v = eventDrawingArea.getChildAt(i);
                if (v.getTag() != null && v.getTag().toString().startsWith("day_separator_")) {
                    eventDrawingArea.removeView(v);
                }
            }
            // Vẽ line đầu, năm columntime

            View line = new View(requireContext());
            line.setTag("day_separator_" );
            line.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.very_light));

            ConstraintLayout.LayoutParams p = new ConstraintLayout.LayoutParams(
                    (int) (1 * density),
                    ConstraintLayout.LayoutParams.MATCH_PARENT
            );

            p.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
            p.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
            p.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
            line.setLayoutParams(p);
            eventDrawingArea.addView(line, p);
        });
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
        Calendar dayIterator = (Calendar) currentWeekStart.clone();
        TextView todayView = null;

        for (int i = 0; i < DAYS_IN_WEEK; i++) {
            int day = dayIterator.get(Calendar.DAY_OF_MONTH);
            int dayOfMonth = dayIterator.get(Calendar.DAY_OF_MONTH);
            int monthIndex = dayIterator.get(Calendar.MONTH);
            int year = dayIterator.get(Calendar.YEAR);

            // i là cellIndex (0 đến 6)
            TextView tvDay = createWeekDayTextView(
                    String.valueOf(day),
                    true,
                    i,
                    dayOfMonth,
                    monthIndex,
                    year
            );

            gridWeekDays.addView(tvDay);
            if (checkIsToday(dayOfMonth, monthIndex, year)) {
                todayView = tvDay; // Lưu lại TextView của ngày hôm nay
            }

            // Tăng ngày lên 1 để lặp tiếp
            dayIterator.add(Calendar.DAY_OF_MONTH, 1);

            if (todayView != null) {
                lastSelectedDayView = todayView; // Gán TextView của ngày hôm nay
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

        if (isActualDay) {
            tv.setOnClickListener(v -> {
                clearAllHighlight();
                highlightSelectedDay(tv);

                lastSelectedDayView = tv;
                selectedDayOfMonth = dayOfMonth;
                loadEventsForSelectedDay();
            });
        }
        return tv;
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
        boolean isToday = (dayOfMonth == todayDay) && (monthIndex == todayMonth) && (year == todayYear);

        if (isToday) {
            tv.setBackgroundResource(R.drawable.bg_current_day_for_month);
            tv.setTextColor(Color.WHITE);
            tv.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
            tv.setBackgroundResource(R.drawable.bg_select_day_for_month);
            tv.setTextColor(Color.WHITE);
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
    private void clearAllHighlight() {
        for (int i = 0; i < gridWeekDays.getChildCount(); i++) {
            View child = gridWeekDays.getChildAt(i);
            if (child instanceof TextView) {
                resetDayStyle((TextView) child);
            }
        }
    }
    private void resetDayStyle(TextView tv) {
        Object t = tv.getTag();
        int column = 0;
        int dayOfMonth = 0;
        int monthIndex = 0;
        int year = 0;

        if (t instanceof int[]) {
            int[] data = (int[]) t;
            column = data[0];
            dayOfMonth = data[1];
            monthIndex = data[2];
            year = data[3];
        }

        boolean isToday = checkIsToday(dayOfMonth, monthIndex, year);
        applyDefaultDayStyle(tv, true, isToday, column);
    }
    private boolean checkIsToday(int dayOfMonth, int monthIndex, int year) {
        Calendar today = Calendar.getInstance();
        return (dayOfMonth == today.get(Calendar.DAY_OF_MONTH)) &&
                (monthIndex == today.get(Calendar.MONTH)) &&
                (year == today.get(Calendar.YEAR));
    }

    private void loadEventsForSelectedDay() {
        if (db == null || lastSelectedDayView == null) {
            return;
        }

        Object tagObj = lastSelectedDayView.getTag();
        if (!(tagObj instanceof int[])) {
            return;
        }

        int[] tagData = (int[]) tagObj;
        int dayOfMonth = tagData[1];
        int monthIndex = tagData[2];
        int year = tagData[3];

        // Tạo calendar cho ngày được chọn
        Calendar selectedDay = Calendar.getInstance();
        selectedDay.set(year, monthIndex, dayOfMonth, 0, 0, 0);
        selectedDay.set(Calendar.MILLISECOND, 0);

        long dayStartMillis = selectedDay.getTimeInMillis();

        // Query events trong background thread
        Executors.newSingleThreadExecutor().execute(() -> {
            List<EventEntity> events = db.eventDao().getEventsByDay(dayStartMillis);

            // Vẽ events trên UI thread
            if (getView() != null) {
                getView().post(() -> {
                    // Xóa các events cũ (giữ lại grid lines)
                    clearOldEvents();

                    // Vẽ events mới
                    drawEventsForDay(events);
                });
            }
        });
    }
    private void clearOldEvents() {
        for (int i = eventDrawingArea.getChildCount() - 1; i >= 0; i--) {
            View v = eventDrawingArea.getChildAt(i);
            Object tag = v.getTag();
            if (tag != null && tag.toString().startsWith("event_")) {
                eventDrawingArea.removeView(v);
            }
        }
    }
    private void drawEventsForDay(List<EventEntity> events) {
        eventDrawingArea.post(() -> {
            if (!isAdded() || eventDrawingArea.getWidth() == 0) {
                return;
            }

            float density = getResources().getDisplayMetrics().density;
            float hourHeightPx = HOUR_HEIGHT_DP * density;

            int width = eventDrawingArea.getWidth();

            // Tính toán margin
            float totalMarginPx = EVENT_SIDE_MARGIN_DP * 2 * density;
            float leftMarginPx = EVENT_SIDE_MARGIN_DP * density;

            float eventWidth = width - totalMarginPx;

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
                if (height < 20 * density) height = 20 * density; // minimum height

                // TẠO EVENT BOX
                TextView eventView = new TextView(requireContext());
                eventView.setText(e.title);
                eventView.setTextSize(12);
                eventView.setTextColor(Color.WHITE);
                eventView.setGravity(Gravity.TOP | Gravity.START);
                eventView.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
                eventView.setPadding(12, 8, 12, 8);
                eventView.setTag("event_" + e.id);

                Drawable gradient = Objects.requireNonNull(
                        ContextCompat.getDrawable(requireContext(), COLOR_MAPPING_DRAWABLE[e.color])
                ).mutate();

                // Lấy layout shape (bo tròn, stroke…)
                GradientDrawable shape = (GradientDrawable) Objects.requireNonNull(
                        ContextCompat.getDrawable(requireContext(), R.drawable.bg_event_shape)
                ).mutate();

                // Ép màu từ gradient vào shape
                if (gradient instanceof GradientDrawable) {
                    int[] colors = ((GradientDrawable) gradient).getColors();
                    if (colors != null) {
                        shape.setColors(colors);
                    }
                }

                // Gán vào view
                eventView.setBackground(shape);
                ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(
                        (int) eventWidth,
                        (int) height
                );

                lp.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
                lp.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;

                eventView.setLayoutParams(lp);

                // Đặt vị trí cho event
                eventView.setTranslationX(leftMarginPx);
                eventView.setTranslationY(start);

                eventDrawingArea.addView(eventView);
            }
        });

    }
}