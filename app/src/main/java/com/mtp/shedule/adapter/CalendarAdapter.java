package com.mtp.shedule.adapter;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.mtp.shedule.fragment.calendarfragment.DayViewFragment;
import com.mtp.shedule.fragment.calendarfragment.MonthViewFragment;
import com.mtp.shedule.fragment.calendarfragment.WeekViewFragment;
import com.mtp.shedule.fragment.calendarfragment.YearViewFragment;

public class CalendarAdapter extends FragmentStateAdapter {

    private final Fragment[] fragments = new Fragment[4];

    public CalendarAdapter(@NonNull Fragment parentFragment) {
        super(parentFragment);

        fragments[0] = new YearViewFragment();
        fragments[1] = new MonthViewFragment();
        fragments[2] = new WeekViewFragment();
        fragments[3] = new DayViewFragment();
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return fragments[position];
    }

    @Override
    public int getItemCount() {
        return 4;
    }
}
