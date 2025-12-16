package com.mtp.shedule.interaction;

import android.os.Bundle;

public interface CalendarInteractionListener {
    /**
     * Chuyển ViewPager sang một vị trí mới (tab)
     * @param position Vị trí tab mới (0=Year, 1=Month, 2=Week, 3=Day)
     * @param data Dữ liệu tùy chọn cần truyền (ví dụ: tháng đã chọn)
     */
    void onSwitchTo(int position, Bundle data);
}
