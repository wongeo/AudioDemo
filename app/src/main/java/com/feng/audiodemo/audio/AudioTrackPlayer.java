package com.feng.audiodemo.audio;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

public class AudioTrackPlayer {
    public static final String TAG = "AudioTrackPlayer";

    private Context mContext;
    private AudioTrack mAudioTrack;

    public AudioTrackPlayer(Context context) {
        mContext = context;
    }

    public void play(String uri) {
        this.stop();
        byte[] data = new byte[264848];
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .build();
        AudioFormat audioFormat = new AudioFormat.Builder()
                .setSampleRate(8000)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .build();
        mAudioTrack = new AudioTrack(attributes, audioFormat, data.length, AudioTrack.MODE_STATIC, AudioManager.AUDIO_SESSION_ID_GENERATE);
        Log.d(TAG, "Writing audio data...");
        InputStream fis = null;
        try {
            fis = new FileInputStream(new File(uri));
            while (fis.read(data, 0, data.length) != -1) {
                this.mAudioTrack.write(data, 0, data.length);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            close(fis);
        }

        mAudioTrack.play();
        Log.d(TAG, "Playing");
    }

    public void stop() {
        if (this.mAudioTrack != null) {
            Log.d(TAG, "Stopping");
            mAudioTrack.stop();
            Log.d(TAG, "Releasing");
            mAudioTrack.release();
            Log.d(TAG, "Nulling");
        }
    }

    private static void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Throwable throwable) {

        }
    }
}
