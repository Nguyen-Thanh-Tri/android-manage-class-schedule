package com.mtp.shedule.fragment;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.mtp.shedule.fragment.calendarfragment.MonthViewFragment;
import com.mtp.shedule.interaction.CalendarInteractionListener;
import com.mtp.shedule.R;
import com.mtp.shedule.adapter.CalendarAdapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class CalendarFragment extends Fragment implements CalendarInteractionListener {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private CalendarAdapter adapter;
    private static final int TARGET_YEAR = 2025;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_calendar, container, false);

        tabLayout = view.findViewById(R.id.tabLayout);
        viewPager = view.findViewById(R.id.viewPager);

        adapter = new CalendarAdapter(this);
        viewPager.setAdapter(adapter);

        viewPager.setUserInputEnabled(false);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("Year");
                    break;
                case 1:
                    tab.setText("Month");
                    break;
                case 2:
                    tab.setText("Week");
                    break;
                case 3:
                    tab.setText("Day");
                    break;
            }
        }).attach();

        setupTabIndicatorAnimation();

        return view;
    }

    // Trong CalendarFragment.java

    @Override
    public void onSwitchTo(int position, Bundle data) {
        if (position == 1 && data != null) {
            int selectedMonth = data.getInt("SELECTED_MONTH_INDEX");
            int selectedYear = data.getInt("SELECTED_YEAR", TARGET_YEAR);

            // Chuyển ViewPager đến vị trí tab mới (Tab Month)
            viewPager.setCurrentItem(position,false);

            viewPager.post(() -> {
                Fragment fragment = getChildFragmentManager().findFragmentByTag("f" + position);

                if (fragment instanceof MonthViewFragment) {
                    ((MonthViewFragment) fragment).updateMonth(selectedMonth, selectedYear);
                }
            });
        }
    }

    private void setupTabIndicatorAnimation() {
        int orange = ContextCompat.getColor(requireContext(), R.color.orange);
        int grey = ContextCompat.getColor(requireContext(), R.color.grey);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            private int lastPosition = 0;

            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);

                // Mỗi khi chuyển tab → animate màu từ grey → orange
                ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), grey, orange);
                colorAnim.setDuration(300); // thời gian hiệu ứng
                colorAnim.addUpdateListener(animator -> {
                    int color = (int) animator.getAnimatedValue();
                    tabLayout.setSelectedTabIndicatorColor(color);
                });
                colorAnim.start();

                lastPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                // Khi bắt đầu vuốt → chuyển tạm sang grey
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    tabLayout.setSelectedTabIndicatorColor(grey);
                }
            }
        });
    }
}
