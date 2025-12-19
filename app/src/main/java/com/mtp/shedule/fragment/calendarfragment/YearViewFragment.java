package com.mtp.shedule.fragment.calendarfragment;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.gridlayout.widget.GridLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.mtp.shedule.interaction.CalendarInteractionListener;
import com.mtp.shedule.R;

import java.util.Calendar;
import java.util.Locale;


public class YearViewFragment extends Fragment {
    private CalendarInteractionListener listener;
    private int displayYear;
    Calendar calendar = Calendar.getInstance();
    int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
    int currentMonth = calendar.get(Calendar.MONTH);
    int currentYear = calendar.get(Calendar.YEAR);
    // View components
    private GridLayout gridMonths;
    private TextView tvYear;
    private LayoutInflater mInflater;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Mặc định lấy năm hiện tại khi mở App
        displayYear = currentYear;
    }
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Lắng nghe từ Fragment cha (CalendarFragment)
        if (getParentFragment() instanceof CalendarInteractionListener) {
            listener = (CalendarInteractionListener) getParentFragment();
        } else {
            // Đây là một RuntimeException cần thiết nếu không implement
            throw new RuntimeException(context.toString() + " must implement CalendarInteractionListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_year_view, container, false);
        mInflater = inflater;

        gridMonths = view.findViewById(R.id.gridMonths);
        tvYear = view.findViewById(R.id.tvYear);

        tvYear.setOnClickListener(v -> showYearPickerDialog());

        refreshYearView();

        return view;
    }
    private void refreshYearView() {
        tvYear.setText(String.format(Locale.getDefault(), "%d Year", displayYear));
        gridMonths.removeAllViews(); // Xóa sạch lịch cũ

        // Lặp qua 12 ô tháng
        for (int i = 0; i < 12; i++) {
            View monthCell = mInflater.inflate(R.layout.mini_month_cell, gridMonths, false);

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            params.width = 0;
            params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.setMargins(8, 8, 8, 24);
            monthCell.setLayoutParams(params);

            gridMonths.addView(monthCell);

            // Fill Data
            populateMonth(i, monthCell, displayYear);

            // Click direct to MonthView
            final int selectedMonth = i;
            monthCell.setOnClickListener(v -> {
                if (listener != null) {
                    Bundle data = new Bundle();
                    data.putInt("SELECTED_MONTH_INDEX", selectedMonth);
                    data.putInt("SELECTED_YEAR", displayYear);

                    listener.onSwitchTo(1, data);
                }
            });
            // ----------------------------------------------------
        }
    }
    private void showYearPickerDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = mInflater.inflate(R.layout.dialog_only_year_picker, null);
        builder.setView(dialogView);

        final NumberPicker yearPicker = dialogView.findViewById(R.id.pickerYear);

        // Cấu hình chọn năm (Ví dụ: +/- 100 năm so với hiện tại)
        yearPicker.setMinValue(1900);
        yearPicker.setMaxValue(2100);
        yearPicker.setValue(displayYear);
        yearPicker.setWrapSelectorWheel(false);

        builder.setPositiveButton("OK", (dialog, which) -> {
            displayYear = yearPicker.getValue();
            // Vẽ lại lưới lịch
            refreshYearView();
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
        builder.create().show();
    }
    private void populateMonth(int monthIndex, View monthCellView, int year) {
        TextView tvTitle = monthCellView.findViewById(R.id.tvMonthTitle);
        GridLayout gridDays = monthCellView.findViewById(R.id.gridDaysInMonth);

        gridDays.setColumnCount(7);

        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        tvTitle.setText(monthNames[monthIndex].toUpperCase(Locale.getDefault()));

        boolean isCurrentMonth = (monthIndex == currentMonth) && (year == currentYear);
        if (isCurrentMonth) {
            tvTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.azure)); // blue
        } else {
            tvTitle.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            tvTitle.setBackground(null);
        }

        gridDays.removeAllViews();

        // TÍNH TOÁN LỊCH
        calendar.set(year, monthIndex, 1); // Đặt về ngày 1 của tháng cần vẽ

        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK); // Ngày bắt đầu: 1=CN, 2=T2, ...
        int maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH); // Tổng số ngày trong tháng

        // Chuyển đổi DayOfWeek để bắt đầu từ Thứ Hai (hoặc Chủ Nhật)
        // Nếu bạn muốn T2 là cột đầu tiên: dayOffset = dayOfWeek - 2. Nếu CN đầu tiên: dayOffset = dayOfWeek - 1.
        // Giả sử Lịch bắt đầu bằng Chủ Nhật:
        int dayOffset = dayOfWeek - 1;

        //  VẼ CÁC Ô TRỐNG ĐẦU THÁNG
        for (int i = 0; i < dayOffset; i++) {
            gridDays.addView(createDayTextView("", false, false));
        }
        // VẼ CÁC NGÀY TRONG THÁNG
        for (int day = 1; day <= maxDays; day++) {
            boolean isToday = (day == currentDay) &&
                    (monthIndex == currentMonth) &&
                    (year == currentYear);
            gridDays.addView(createDayTextView(String.valueOf(day), true, isToday));
        }
    }

    // HÀM TẠO TEXTVIEW CHO NGÀY
    private TextView createDayTextView(String text, boolean isActualDay, boolean isToday) {
        TextView tv = new TextView(requireContext());

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();

        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f); // Trọng số 1 cho cột
        params.width = 0;

        //ĐẶT TRỌNG SỐ HÀNG (MỚI)
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED);
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;

        tv.setLayoutParams(params);
        // Cài đặt kiểu chữ
        tv.setText(text);
        tv.setTypeface(Typeface.SANS_SERIF);
        tv.setTextSize(10.5f);
        tv.setGravity(Gravity.CENTER);

        // Xử lý màu sắc
        if (isActualDay) {
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            if (isToday) {
                tv.setBackgroundResource(R.drawable.bg_current_day_for_year);
                tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.black));
            } else {
                tv.setBackground(null);
            }
        } else {
            // Ô trống
            tv.setTextColor(ContextCompat.getColor(requireContext(), R.color.transparent));
        }
        return tv;
    }
}
