package com.mtp.shedule.fragment.calendarfragment;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
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
import androidx.gridlayout.widget.GridLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mtp.shedule.R;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Locale;

public class MonthViewFragment extends Fragment {
    // Enum to manage the display state of the calendar
    private enum ViewState implements Serializable {
        MONTH_VIEW,
        SPLIT_VIEW,
        WEEK_VIEW
    }

    private int currentMonthIndex;
    private int currentYear;
    private TextView tvMonthHeader;
    private GridLayout gridFullMonthDays;
    private ImageView ivDragHandle;
    private LinearLayout llEventArea;

    private ViewState currentState = ViewState.SPLIT_VIEW; // Default state is balanced

    // Variables for drag logic
    private float initialTouchY;
    private float initialCalendarWeight;
    private float initialEventWeight;

    // Height of one row (calculated when layout is complete)
    private int rowHeight = 0;

    // Constants
    private static final int DAYS_IN_WEEK = 7;
    private static final int MAX_ROWS = 6;
    private boolean isDragging = false;


    // Biến để lưu số hàng thực tế của tháng hiện tại
    private int actualRowsNeeded = MAX_ROWS;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Calendar now = Calendar.getInstance();
        currentMonthIndex = now.get(Calendar.MONTH);
        currentYear = now.get(Calendar.YEAR);

        if (getArguments() != null) {
            currentMonthIndex = getArguments().getInt("INITIAL_MONTH_INDEX", currentMonthIndex);
            currentYear = getArguments().getInt("INITIAL_YEAR", currentYear);
        }

        if (savedInstanceState != null) {
            currentMonthIndex = savedInstanceState.getInt("state_month_index", currentMonthIndex);
            currentYear = savedInstanceState.getInt("state_year", currentYear);

            // FIX: Use the standard getSerializable(String key)
            Object viewStateObject = savedInstanceState.getSerializable("state_view");
            if (viewStateObject instanceof ViewState) {
                currentState = (ViewState) viewStateObject;
            } else {
                currentState = ViewState.SPLIT_VIEW; // Safe default value
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Hiện tại, ta dùng placeholder và gọi hàm hiển thị lịch
        View view = inflater.inflate(R.layout.fragment_month_view, container, false);

        tvMonthHeader = view.findViewById(R.id.tvMonthHeader);
        gridFullMonthDays = view.findViewById(R.id.gridFullMonthDays);
        ivDragHandle = view.findViewById(R.id.ivDragHandle);
        llEventArea = view.findViewById(R.id.llEventArea);

        setupDragHandle();
        displayCalendar();

        // Thiết lập trạng thái ban đầu dựa trên currentState
        view.post(() -> updateViewState(currentState, false));

        return view;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("state_month_index", currentMonthIndex);
        outState.putInt("state_year", currentYear);
        outState.putSerializable("state_view", currentState);
    }

    public void updateMonth(int newMonthIndex, int year) {
        this.currentMonthIndex = newMonthIndex;
        this.currentYear = year;

        if (getView() != null) {
            displayCalendar();
            // Đảm bảo cập nhật lại trạng thái xem sau khi vẽ lại lịch
            updateViewState(currentState, false);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupDragHandle() {
        ivDragHandle.setOnClickListener(v -> {
            // Simple click handling: toggle between WEEK_VIEW and SPLIT_VIEW
            if (currentState == ViewState.WEEK_VIEW) {
                updateViewState(ViewState.SPLIT_VIEW, true);
            } else {
                updateViewState(ViewState.WEEK_VIEW, true);
            }
        });

        // Simple drag/drop logic
        ivDragHandle.setOnTouchListener((v, event) -> {
            LinearLayout.LayoutParams calendarParams = (LinearLayout.LayoutParams) gridFullMonthDays.getLayoutParams();
            LinearLayout.LayoutParams eventParams = (LinearLayout.LayoutParams) llEventArea.getLayoutParams();

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
                    llEventArea.setLayoutParams(eventParams);
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
                                // Kéo nhẹ xuống -> về SPLIT_VIEW
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
                calendarWeight = 100f / MAX_ROWS;
                eventWeight = 100f - calendarWeight;
                dragHandleIcon = R.drawable.ic_drag_handle_down;

                Calendar calendar = Calendar.getInstance();
                int todayDay = calendar.get(Calendar.DAY_OF_MONTH);
                int todayMonth = calendar.get(Calendar.MONTH);
                int todayYear = calendar.get(Calendar.YEAR);

                if (currentMonthIndex == todayMonth && currentYear == todayYear) {
                    calendar.set(currentYear, currentMonthIndex, 1);
                    int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
                    int dayOffset = firstDayOfWeek - 1;
                    int cellIndex = dayOffset + todayDay - 1;
                    targetRowIndex = cellIndex / DAYS_IN_WEEK;
                } else targetRowIndex = 0;
                break;
            case SPLIT_VIEW:
            default:
                calendarWeight = 70f;
                eventWeight = 30f;
                dragHandleIcon = R.drawable.ic_drag_handle_normal;
                break;
        }

        ivDragHandle.setImageResource(dragHandleIcon);

        LinearLayout.LayoutParams calendarParams = (LinearLayout.LayoutParams) gridFullMonthDays.getLayoutParams();
        LinearLayout.LayoutParams eventParams = (LinearLayout.LayoutParams) llEventArea.getLayoutParams();

        if (animate) {
            ValueAnimator animator = ValueAnimator.ofFloat(calendarParams.weight, calendarWeight);
            animator.setDuration(300);
            animator.addUpdateListener(animation -> {
                float animatedWeight = (float) animation.getAnimatedValue();
                calendarParams.weight = animatedWeight;
                eventParams.weight = 100f - animatedWeight;
                gridFullMonthDays.setLayoutParams(calendarParams);
                llEventArea.setLayoutParams(eventParams);

                if (newState == ViewState.MONTH_VIEW || animatedWeight > 95) {
                    llEventArea.setVisibility(View.GONE);
                } else llEventArea.setVisibility(View.VISIBLE);
            });
            animator.start();
        } else {
            calendarParams.weight = calendarWeight;
            eventParams.weight = eventWeight;
            gridFullMonthDays.setLayoutParams(calendarParams);
            llEventArea.setLayoutParams(eventParams);
            llEventArea.setVisibility(eventWeight > 0 ? View.VISIBLE : View.GONE);
        }

        // WEEK_VIEW fade effect
        if (newState == ViewState.WEEK_VIEW) {
            int finalTargetRow = targetRowIndex;
            for (int i = 0; i < gridFullMonthDays.getChildCount(); i++) {
                View child = gridFullMonthDays.getChildAt(i);
                Object tag = child.getTag();
                if (tag instanceof Integer) {
                    int row = (int) tag;
                    if (row == finalTargetRow) {
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
    }



    private void displayCalendar() {
        String monthName = getMonthName(currentMonthIndex);
        tvMonthHeader.setText(String.format(Locale.getDefault(), "%s %d", monthName, currentYear));

        gridFullMonthDays.removeAllViews();

        Calendar calendar = Calendar.getInstance();
        calendar.set(currentYear, currentMonthIndex, 1);
        calendar.set(Calendar.DAY_OF_MONTH, 1);

        // Get today's info to highlight
        Calendar today = Calendar.getInstance();
        int todayDay = today.get(Calendar.DAY_OF_MONTH);
        int todayMonth = today.get(Calendar.MONTH);
        int todayYear = today.get(Calendar.YEAR);

        int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK); // 1=Sunday
        int dayOffset = firstDayOfWeek - 1;

        int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        int cellIndex = 0;

        int totalCellsNeeded = dayOffset + daysInMonth;
        actualRowsNeeded = (int) Math.ceil((double) totalCellsNeeded / DAYS_IN_WEEK);
        // DRAW EMPTY CELLS AT THE BEGINNING OF THE MONTH
        for (int i = 0; i < dayOffset; i++) {
            gridFullMonthDays.addView(createMonthDayTextView("", false, cellIndex++));
        }

        // DRAW DAYS OF THE MONTH
        for (int day = 1; day <= daysInMonth; day++) {
            boolean isToday = (day == todayDay &&
                    currentMonthIndex == todayMonth &&
                    currentYear == todayYear);
            gridFullMonthDays.addView(createMonthDayTextView(String.valueOf(day), true, cellIndex++));
        }

        int cellsToFill = actualRowsNeeded * DAYS_IN_WEEK;
        for (int i = cellIndex; i < cellsToFill; i++) {
            // isToday luôn là false cho ô trống
            gridFullMonthDays.addView(createMonthDayTextView("", false, cellIndex++));
        }

        // Sau khi vẽ lại lịch, áp dụng lại trạng thái xem hiện tại (để ẩn/hiện nếu đang ở WEEK_VIEW)
        if (getView() != null) {
            updateViewState(currentState, false);
        }
    }


    private TextView createMonthDayTextView(String text, boolean isActualDay, int cellIndex) {
        TextView tv = new TextView(requireContext());

        // Tính toán vị trí Hàng và Cột
        int row = cellIndex / DAYS_IN_WEEK;
        int column = cellIndex % DAYS_IN_WEEK;
        // Gắn tag là chỉ số hàng (row index). Đây là bước quan trọng để xác định ô thuộc hàng nào.
        tv.setTag(row);
        // TÍNH TOÁN TRỌNG SỐ HÀNG (weight)
        // Dùng 1.0f chia cho số hàng thực tế để chia đều chiều cao.
        float rowWeight = 1.0f / actualRowsNeeded;

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();

        params.columnSpec = GridLayout.spec(column,1,  1f); // Cột tự động, chiếm 1 cột, weight 1f
        params.width = 0;

        // 3. CHIA ĐỀU HÀNG: Thiết lập rowSpec để View chiếm 1 hàng và chia đều trọng số (weight)
        params.rowSpec = GridLayout.spec(row, rowWeight); // Hàng tự động, weight 1f
        params.height = 0;

        // Căn chỉnh lề cho đẹp hơn
        int margin = 4;
        params.setMargins(margin, margin, margin, margin);
        // *******************************************

        tv.setLayoutParams(params);

        tv.setText(text);
        tv.setTypeface(Typeface.SANS_SERIF);
        tv.setTextSize(18f);
        tv.setGravity(Gravity.CENTER);

        boolean isToday = false;
        if (isActualDay) {
            Calendar today = Calendar.getInstance();
            // Đảm bảo chỉ parse khi text không rỗng
            try {
                int day = Integer.parseInt(text);
                isToday = (day == today.get(Calendar.DAY_OF_MONTH)) &&
                        (currentMonthIndex == today.get(Calendar.MONTH)) &&
                        (currentYear == today.get(Calendar.YEAR));
            } catch (NumberFormatException e) {
                // Should not happen for isActualDay=true, but defensive programming
            }
        }

        if (isActualDay) {
            tv.setTextColor(Color.BLACK);
            if (isToday) {
                // Assume this drawable exists
                tv.setBackgroundResource(R.drawable.bg_current_day_for_month);
                tv.setTextColor(Color.WHITE);
                tv.setTypeface(Typeface.DEFAULT_BOLD);
            } else {
                tv.setBackground(null);
            }
        } else {
            tv.setTextColor(Color.TRANSPARENT);
        }

        // Add click event for testing
        tv.setOnClickListener(v -> {
            if (isActualDay) {
                // Handle day click
            }
        });

        return tv;
    }

    private String getMonthName(int monthIndex) {
        // Use Localized String instead of hardcode
        Calendar tempCal = Calendar.getInstance();
        tempCal.set(Calendar.MONTH, monthIndex);
        return tempCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale.getDefault());
    }
}