package com.mtp.shedule.fragment;

import com.mtp.shedule.helper.UpdateChecker;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.mtp.shedule.R;
import com.mtp.shedule.utils.AppSettings;
import com.mtp.shedule.BuildConfig;


public class SettingsFragment extends Fragment {

    private LinearLayout btnDarkMode, btnRingtone;
    private TextView tvCurrentTheme, tvRingtoneName;
    private AppSettings settings;
    private MediaPlayer previewPlayer; // Để nghe thử tiếng mèo
    private boolean isThemeDialogShowing = false;

    // Launcher để hứng kết quả khi người dùng chọn nhạc chuông xong
    // Launcher cho System Ringtone Picker (khi user chọn dòng cuối)
    private final ActivityResultLauncher<Intent> systemRingtoneLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);
                    if (uri != null) {
                        String name = RingtoneManager.getRingtone(getContext(), uri).getTitle(getContext());
                        saveAndUpdateUI(uri, name);
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        settings = new AppSettings(requireContext());

        btnDarkMode = view.findViewById(R.id.btnDarkMode);
        btnRingtone = view.findViewById(R.id.btnRingtone);
        tvCurrentTheme = view.findViewById(R.id.tvCurrentTheme);
        tvRingtoneName = view.findViewById(R.id.tvRingtoneName);
        TextView tvAppVersion = view.findViewById(R.id.tvAppVersion);
        LinearLayout btnCheckUpdate = view.findViewById(R.id.btnCheckUpdate);

        tvAppVersion.setText(BuildConfig.VERSION_NAME);
        btnCheckUpdate.setOnClickListener(v -> {
            // Gọi class UpdateChecker mà chúng ta vừa tạo
            UpdateChecker.checkForUpdate(requireActivity());
        });

        setupUI();

        btnDarkMode.setOnClickListener(v -> showThemeDialog());
        btnRingtone.setOnClickListener(v -> showCustomRingtoneDialog());

        //Kiểm tra trạng thái cũ. Nếu trước đó Dialog đang mở -> Mở lại ngay lập tức
        if (savedInstanceState != null) {
            isThemeDialogShowing = savedInstanceState.getBoolean("STATE_THEME_DIALOG", false);
            if (isThemeDialogShowing) {
                showThemeDialog();
            }
        }

        return view;
    }
    //Lưu trạng thái trước khi màn hình bị hủy (do đổi theme)
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("STATE_THEME_DIALOG", isThemeDialogShowing);
    }

    private void showCustomRingtoneDialog() {
        String[] options = {"Meow 1 ", "Meow 2", "Meow 3", "Choose ringtone from system"};
        int[] rawIds = {R.raw.cat1, R.raw.cat2, R.raw.cat3};

        // 1. Xác định vị trí đang được chọn hiện tại (để đánh dấu tick ban đầu)
        int currentSelection = -1;
        Uri currentUri = settings.getRingtoneUri();
        String currentUriString = (currentUri != null) ? currentUri.toString() : "";

        // Kiểm tra xem nhạc hiện tại có phải là một trong 3 bài mèo không
        for (int i = 0; i < rawIds.length; i++) {
            String path = "android.resource://" + requireContext().getPackageName() + "/" + rawIds[i];
            if (currentUriString.equals(path)) {
                currentSelection = i;
                break;
            }
        }
        // Nếu không phải mèo (đang là nhạc hệ thống), thì không tick vào bài mèo nào cả (hoặc tick vào mục cuối)
        if (currentSelection == -1 && !currentUriString.contains("android.resource")) {
            currentSelection = 3; // Index của dòng "Chọn từ hệ thống..."
        }

        // Biến mảng 1 phần tử để lưu vị trí tạm thời khi người dùng click qua lại (nhưng chưa Save)
        final int[] tempSelection = {currentSelection};

        AlertDialog dialog = new AlertDialog.Builder(getContext(), R.style.RoundedAlertDialog)
                .setTitle("Select Ringtone")
                // setSingleChoiceItems tạo ra list có dấu tick tròn bên phải
                .setSingleChoiceItems(options, currentSelection, (dialogInterface, which) -> {
                    // Cập nhật vị trí đang chọn tạm thời
                    tempSelection[0] = which;

                    if (which < 3) {
                        // NẾU CHỌN MÈO: Nghe thử ngay lập tức
                        int selectedResId = rawIds[which];
                        Uri catUri = Uri.parse("android.resource://" + requireContext().getPackageName() + "/" + selectedResId);

                        // Stop bài cũ (nếu có) và phát bài mới
                        playPreview(catUri);
                    } else {
                        // NẾU CHỌN HỆ THỐNG:
                        // Dừng nhạc mèo
                        stopPreview();
                        // Mở trình chọn hệ thống ngay (vì trình hệ thống là Activity khác, buộc phải đóng dialog này)
                        dialogInterface.dismiss();
                        openSystemRingtonePicker();
                    }
                })
                .setPositiveButton("Save", (dialogInterface, which) -> {
                    // CHỈ KHI BẤM LƯU MỚI THỰC HIỆN LƯU VÀO SETTING
                    int selectedIndex = tempSelection[0];

                    if (selectedIndex >= 0 && selectedIndex < 3) {
                        // Lưu bài Mèo
                        int selectedResId = rawIds[selectedIndex];
                        String name = options[selectedIndex];
                        Uri catUri = Uri.parse("android.resource://" + requireContext().getPackageName() + "/" + selectedResId);

                        saveAndUpdateUI(catUri, name);
                    }
                    // Nếu selectedIndex == 3 (Hệ thống) thì đã được xử lý ở phần click item rồi.

                    stopPreview(); // Dừng nhạc khi đóng
                })
                .setNegativeButton("Cancel", (dialogInterface, which) -> {
                    //dừng nhạc
                    stopPreview();
                })
                .create();

        // bấm ra ngoài dialog (OnDismiss)
        dialog.setOnDismissListener(dialogInterface -> stopPreview());

        dialog.show();
    }
    private void openSystemRingtonePicker() {
        Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select System Tone");
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true);

        // Nếu đang chọn nhạc hệ thống thì tick sẵn
        Uri currentUri = settings.getRingtoneUri();
        if (currentUri != null && !currentUri.toString().contains("android.resource")) {
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, currentUri);
        }

        systemRingtoneLauncher.launch(intent);
    }

    private void saveAndUpdateUI(Uri uri, String name) {
        settings.saveRingtone(uri, name);
        tvRingtoneName.setText(name);
    }
    private void setupUI() {
        // Cập nhật text Theme hiện tại
        int mode = settings.getThemeMode();
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) tvCurrentTheme.setText("Light");
        else if (mode == AppCompatDelegate.MODE_NIGHT_YES) tvCurrentTheme.setText("Dark");
        else tvCurrentTheme.setText("System");

        // Cập nhật tên nhạc chuông hiện tại
        tvRingtoneName.setText(settings.getRingtoneName());
    }
    // Hàm nghe thử tiếng mèo
    private void playPreview(Uri uri) {
        stopPreview();
        try {
            previewPlayer = new MediaPlayer();
            previewPlayer.setDataSource(getContext(), uri);
            previewPlayer.prepare();
            previewPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void stopPreview() {
        if (previewPlayer != null) {
            if (previewPlayer.isPlaying()) previewPlayer.stop();
            previewPlayer.release();
            previewPlayer = null;
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopPreview(); // Dừng nhạc khi thoát màn hình
    }
    private void showThemeDialog() {
        // Đánh dấu là đang mở dialog
        isThemeDialogShowing = true;

        int nightModeFlags = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        String systemStatus = (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) ? "(Dark)" : "(Light)";

        String[] modes = {"System Default " + systemStatus, "Light Mode", "Dark Mode"};

        int checkedItem;
        int currentMode = settings.getThemeMode();

        if (currentMode == AppCompatDelegate.MODE_NIGHT_NO) checkedItem = 1;
        else if (currentMode == AppCompatDelegate.MODE_NIGHT_YES) checkedItem = 2;
        else checkedItem = 0;

        AlertDialog dialog = new AlertDialog.Builder(getContext(), R.style.RoundedAlertDialog)
                .setTitle("Choose Theme")
                .setSingleChoiceItems(modes, checkedItem, (dialogInterface, which) -> {
                    int selectedMode;
                    if (which == 1) selectedMode = AppCompatDelegate.MODE_NIGHT_NO;
                    else if (which == 2) selectedMode = AppCompatDelegate.MODE_NIGHT_YES;
                    else selectedMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

                    // Lưu và Áp dụng theme
                    settings.saveThemeMode(selectedMode);
                    AppCompatDelegate.setDefaultNightMode(selectedMode);

                    setupUI();
                })
                .setNegativeButton("Exit", (dialogInterface, which) -> {
                    // [Exit thìtắt dialog
                    isThemeDialogShowing = false;
                    dialogInterface.dismiss();
                })
                .create();

        dialog.setOnCancelListener(dialogInterface -> {
            isThemeDialogShowing = false;
        });

        dialog.show();
    }
}