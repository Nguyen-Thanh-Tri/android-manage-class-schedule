package com.mtp.shedule;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.mtp.shedule.R;

import java.util.Objects;

public class SelectColorDialog extends DialogFragment {
    private OnColorSelectedListener listener;
    private View lastSelectedView = null;
    private int selectedColorIndex = 0; // Index mặc định là 0 (Red)
    public interface OnColorSelectedListener {
        void onColorSelected(int drawableId);
    }

    public void setOnColorSelectedListener(OnColorSelectedListener listener) {
        this.listener = listener;
    }
    public void setSelectedColorIndex(int index) {
        this.selectedColorIndex = index;
    }
    public static final int[] COLOR_MAPPING_DRAWABLE = {
            R.drawable.gradient_bg_red,  // Ánh xạ màu #E91E63
            R.drawable.gradient_bg_green, // Ánh xạ màu #4CAF50
            R.drawable.gradient_bg_redorange,   // Ánh xạ màu #F44336
            R.drawable.gradient_bg_orange, // Ánh xạ màu #FF9800
            R.drawable.gradient_bg_yellow, // Ánh xạ màu #FFEB3B
            R.drawable.gradient_bg_purple, // Ánh xạ màu #9C27B0
            R.drawable.gradient_bg_blue,   // Ánh xạ màu #2196F3
            R.drawable.gradient_bg_deeppurple, // Ánh xạ màu #673AB7
            R.drawable.gradient_bg_cyan,   // Ánh xạ màu #00BCD4
            R.drawable.gradient_bg_lightgreen, // Ánh xạ màu #8BC34A
            R.drawable.gradient_bg_deeporange, // Ánh xạ màu #FF5722
            R.drawable.gradient_bg_teal    // Ánh xạ màu #009688
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_select_color, container, false);

        GridLayout grid = view.findViewById(R.id.gridColors);
        TextView btnSave = view.findViewById(R.id.btnSave);

        // Lặp qua mảng GRADIENT DRAWABLE IDs
        for (int i = 0; i < COLOR_MAPPING_DRAWABLE.length; i++) {
            final int currentIndex = i; // Index hiện tại
            final int currentDrawableId = COLOR_MAPPING_DRAWABLE[i]; // Lấy Drawable ID tương ứng

            View colorView = new View(getContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();

            params.columnSpec = GridLayout.spec(i % 6);
            params.width = (int) (getResources().getDisplayMetrics().density * 40); // khoảng 40dp
            params.height = (int) (getResources().getDisplayMetrics().density * 40); // khoảng 40dp
            params.setMargins(8, 8, 8, 8); // Khoảng cách giữa các ô màu

            colorView.setLayoutParams(params);

            // 1. Áp dụng bo góc chung
            colorView.setBackgroundResource(R.drawable.bg_color_item_rounded);
            colorView.setBackground(ContextCompat.getDrawable(requireContext(), currentDrawableId));

            // Đánh dấu màu đã được chọn ban đầu (kiểm tra bằng Gradient ID)
            if (currentIndex == selectedColorIndex) { // So sánh bằng ID drawable đã lưu
                colorView.setForeground(ContextCompat.getDrawable(requireContext(), R.drawable.selected_border));
                lastSelectedView = colorView;
            }

            colorView.setOnClickListener(v -> {
                if (lastSelectedView != null) {
                    lastSelectedView.setForeground(null); // Xóa viền cũ
                }
                v.setForeground(ContextCompat.getDrawable(requireContext(), R.drawable.selected_border));
                lastSelectedView = v; // Cập nhật view cuối cùng đã chọn

                // Cập nhật ID drawable đã chọn
                selectedColorIndex = currentIndex;
            });
            grid.addView(colorView);
        }

        btnSave.setOnClickListener(v -> {
            if (listener != null ) {
                listener.onColorSelected(selectedColorIndex);
            }
            dismiss();
        });

        return view;
    }
}
