package com.mtp.shedule.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.mtp.shedule.fragment.calendarfragment.DayViewFragment;
import com.mtp.shedule.fragment.calendarfragment.MonthViewFragment;
import com.mtp.shedule.fragment.calendarfragment.WeekViewFragment;
import com.mtp.shedule.fragment.calendarfragment.YearViewFragment;

public class CalendarAdapter extends FragmentStateAdapter {

    public CalendarAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new YearViewFragment();
            case 1:
                return new MonthViewFragment();
            case 2:
                return new WeekViewFragment();
            case 3:
                return new DayViewFragment();
            default:
                return new YearViewFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
