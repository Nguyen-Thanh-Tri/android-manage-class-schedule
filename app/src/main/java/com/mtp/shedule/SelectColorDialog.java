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

public class SelectColorDialog extends DialogFragment {
    private int selectedColor = Color.WHITE;
    private OnColorSelectedListener listener;
    private View lastSelectedView = null;

    public interface OnColorSelectedListener {
        void onColorSelected(int color);
    }

    public void setOnColorSelectedListener(OnColorSelectedListener listener) {
        this.listener = listener;
    }

    public void setSelectedColor(int color) {
        this.selectedColor = color;
    }
    private int selectedColorId = R.drawable.gradient_bg_red; // Default
    private static int[] colors = {
            Color.parseColor("#E91E63"), // Index 0: Red
            Color.parseColor("#9C27B0"), // Index 1: Purple
            Color.parseColor("#F44336"), // Index 2: Red-Orange
            Color.parseColor("#FF9800"), // Index 3: Orange
            Color.parseColor("#FFEB3B"), // Index 4: Yellow
            Color.parseColor("#4CAF50"), // Index 5: Green
            Color.parseColor("#2196F3"), // Index 6: Blue
            Color.parseColor("#673AB7"), // Index 7: Deep Purple
            Color.parseColor("#00BCD4"), // Index 8: Cyan
            Color.parseColor("#8BC34A"), // Index 9: Light Green
            Color.parseColor("#FF5722"), // Index 10: Deep Orange
            Color.parseColor("#009688")  // Index 11: Teal
    };
    private static final int[] COLOR_MAPPING_DRAWABLE = {
            R.drawable.gradient_bg_red,  // Ánh xạ màu #E91E63
            R.drawable.gradient_bg_green, // Ánh xạ màu #9C27B0
            R.drawable.gradient_bg_blue,   // Ánh xạ màu #F44336

    };
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_select_color, container, false);

        GridLayout grid = view.findViewById(R.id.gridColors);
        TextView btnSave = view.findViewById(R.id.btnSave);



        for (int color : colors) {
            final int currentColor = color;

            View colorView = new View(getContext());
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 100;
            params.height = 100;
            params.setMargins(10, 10, 10, 10);

            colorView.setLayoutParams(params);
            colorView.setBackgroundColor(currentColor);
            colorView.setClickable(true);

            // Đánh dấu màu đã được chọn ban đầu (nếu có)
            if (currentColor == selectedColor) {
                colorView.setForeground(ContextCompat.getDrawable(getContext(), R.drawable.selected_border));
                lastSelectedView = colorView;
            }

            colorView.setOnClickListener(v -> {
                if (lastSelectedView != null) {
                    lastSelectedView.setForeground(null); // Xóa viền cũ
                }
                v.setForeground(ContextCompat.getDrawable(getContext(), R.drawable.selected_border));
                lastSelectedView = v; // Cập nhật view cuối cùng đã chọn
                selectedColor = currentColor; // Cập nhật màu đã chọn

            });
            grid.addView(colorView);
        }

        btnSave.setOnClickListener(v -> {
            // TÌM VỊ TRÍ CHỌN:
            int index = -1;
            for (int i = 0; i < colors.length; i++) {
                if (colors[i] == selectedColor) {
                    index = i;
                    break;
                }
            }

            if (listener != null && index != -1) {
                if (index < COLOR_MAPPING_DRAWABLE.length) {
                    int selectedDrawableId = COLOR_MAPPING_DRAWABLE[index];
                    // Gọi phương thức mới đã sửa trong interface
                    ((OnColorSelectedListener) listener).onColorSelected(selectedDrawableId);
                } else {
                    // Xử lý lỗi nếu index vượt quá giới hạn (Chỉ xảy ra nếu bạn thêm màu mới mà quên thêm Drawable)
                    // Bạn có thể chọn gửi một ID mặc định ở đây.
                    ((OnColorSelectedListener) listener).onColorSelected(R.drawable.gradient_bg_red);
                }
            }
            dismiss();
        });

        return view;
    }
}
