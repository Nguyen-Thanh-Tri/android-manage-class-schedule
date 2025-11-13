package com.mtp.shedule.fragment.calendarfragment;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
    private static final int TARGET_YEAR = 2025;

    Calendar calendar = Calendar.getInstance();
    int currentDay = calendar.get(Calendar.DAY_OF_MONTH);
    int currentMonth = calendar.get(Calendar.MONTH);
    int currentYear = calendar.get(Calendar.YEAR);

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

        GridLayout grid = view.findViewById(R.id.gridMonths);
        TextView tvYear = view.findViewById(R.id.tvYear);
        tvYear.setText(String.format(Locale.getDefault(), "%d Year", TARGET_YEAR));

        // Lặp qua 12 ô tháng
        for (int i = 0; i < grid.getChildCount(); i++) {

            final View monthCell = grid.getChildAt(i);
            final int monthIndex = i;

            // Điền dữ liệu ngày tháng vào ô
            populateMonth(monthIndex, monthCell, TARGET_YEAR);
            // Gắn sự kiện click (chuyển tab)
            monthCell.setOnClickListener(v -> {
                // TODO: Logic highlight ô đã click (Tùy chọn)

                // Chuyển sang Tab Month (vị trí 1)
                if (listener != null) {
                    Bundle data = new Bundle();
                    data.putInt("SELECTED_MONTH_INDEX", monthIndex);
                    data.putInt("SELECTED_YEAR", TARGET_YEAR);
                    listener.onSwitchTo(1, data);
                }
            });
        }
        return view;
    }

    private void populateMonth(int monthIndex, View monthCellView, int year) {
        // Lấy tham chiếu các View bên trong ô tháng
        TextView tvTitle = monthCellView.findViewById(R.id.tvMonthTitle);
        GridLayout gridDays = monthCellView.findViewById(R.id.gridDaysInMonth);

        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        tvTitle.setText(monthNames[monthIndex].toUpperCase(Locale.getDefault()));

        boolean isCurrentMonth = (monthIndex == currentMonth) && (year == currentYear);
        if (isCurrentMonth) {
            tvTitle.setTextColor(Color.parseColor("#2196F3")); // blue
        } else {
            tvTitle.setTextColor(Color.BLACK);
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
        params.width = 0; // Bắt buộc đặt width=0dp khi sử dụng trọng số

        // 2. ĐẶT TRỌNG SỐ HÀNG (MỚI)
        params.rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;

        tv.setLayoutParams(params);
        // Cài đặt kiểu chữ
        tv.setText(text);
        tv.setTypeface(Typeface.SANS_SERIF);
        tv.setTextSize(10.5f);
        tv.setGravity(Gravity.CENTER);

        // Xử lý màu sắc
        if (isActualDay) {
            tv.setTextColor(Color.BLACK); // Giả định nền tối
            if (isToday) {
                // Đánh dấu nền cho ngày hôm nay (Ví dụ: một hình tròn màu xanh/xanh lá)
                tv.setBackgroundResource(R.drawable.bg_current_day_for_year);
                tv.setTextColor(Color.BLACK); // Tùy chọn: Đổi màu chữ thành đen nếu nền highlight sáng
            } else {
                // Đảm bảo không có background cho các ngày khác
                tv.setBackground(null);
            }
        } else {
            // Ô trống
            tv.setTextColor(Color.TRANSPARENT);
        }
        return tv;
    }
}
