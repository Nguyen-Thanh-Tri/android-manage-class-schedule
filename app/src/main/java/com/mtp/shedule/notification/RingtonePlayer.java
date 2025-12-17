package com.mtp.shedule.notification;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import com.mtp.shedule.R;

public class RingtonePlayer {
    private static final String TAG = "RingtonePlayer";
    private static Ringtone ringtone;
    private static MediaPlayer mediaPlayer;

    /**
     * Phát nhạc chuông
     */
    public static void play(Context context) {
        try {
            stop();
            mediaPlayer = new MediaPlayer();
            Uri soundUri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.cat1);

            mediaPlayer.setDataSource(context, soundUri);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes attributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                mediaPlayer.setAudioAttributes(attributes);
            } else {
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
            }

            // LẶP LẠI LIÊN TỤC
            mediaPlayer.setLooping(true);

            // ÂM LƯỢNG TỐI ĐA
            mediaPlayer.setVolume(1.0f, 1.0f);

            // Chuẩn bị và phát
            mediaPlayer.prepare();
            mediaPlayer.start();

            Log.e(TAG, "Ringtone started playing (looping)");

        } catch (Exception e) {
            Log.e(TAG, "Error playing ringtone: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Dừng nhạc chuông
     */
    public static void stop() {
        try {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    Log.e(TAG, "Ringtone stopped");
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error stopping ringtone: " + e.getMessage());
        }
    }
    /**
     * Kiểm tra xem nhạc có đang phát không
     */
    public static boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }
}