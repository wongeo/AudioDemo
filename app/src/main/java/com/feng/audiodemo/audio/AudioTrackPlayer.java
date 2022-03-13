package com.feng.audiodemo.audio;

import android.content.Context;
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
    private Thread mPlayThread;

    public enum State {
        NONE,
        LOADING,
        START,
        PAUSE,
        ERROR,
        STOP,
    }

    private volatile State mState = State.NONE;

    public AudioTrackPlayer(Context context) {
        mContext = context;
    }

    public static final int RATE_IN_HZ_16K = 16000;
    public static final int RATE_IN_HZ_48K = 48000;

    private String mUri;

    /**
     * 设置播放地址
     */
    public void setDataSource(String uri) {
        mUri = uri;
    }

    /**
     * 准备播放
     */
    public void prepare() {
        mPlayThread = new PlayThread();
        mPlayThread.start();
    }

    private final Object mLock = new Object();

    private void handleTrackPlay(String uri) {

        int sampleRateInHz = RATE_IN_HZ_48K;
        int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

        int bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
        if (bufferSizeInBytes == AudioTrack.ERROR_BAD_VALUE) {
            Log.e(TAG, "Invalid parameter !");
            return;
        }
        Log.i(TAG, "bufferSizeInBytes = " + bufferSizeInBytes + " bytes !");

        byte[] data = new byte[bufferSizeInBytes * 2];

        mAudioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,      //streamType 流类型，AudioManager 中定义了音频的类型，可大致分为 STREAM_MUSIC 、 STREAM_RINHG 等
                sampleRateInHz,                 //sampleRateInHz 采样率，播放的音频每秒有多少次采样
                channelConfig,                  //channelConfig 声道数配置，单声道和双声道
                audioFormat,                    //audioFormat 数据位宽，选择 16bit ，能够兼容所有 Android 设备
                bufferSizeInBytes,              //bufferSizeInBytes 缓冲区大小，通过 AudioTrack.getMinBufferSize 运算得出
                AudioTrack.MODE_STREAM          //mode 播放模式 ： MODE_STATIC 一次写入，MODE_STREAM 多次写入
        );
        if (mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
            Log.e(TAG, "初始化失败 !");
            return;
        }

        Log.d(TAG, "开始填充数据...");
        InputStream fis = null;
        try {
            fis = new FileInputStream(new File(uri));
            Log.d(TAG, "playing");
            while (fis.read(data, 0, data.length) > 0) {
                synchronized (mLock) {
                    if (mState == State.PAUSE) {
                        Log.d(TAG, "pause");
                        mLock.wait();
                        Log.d(TAG, "start");
                    } else if (mState == State.STOP) {
                        break;
                    }
                    mState = State.START;
                    mAudioTrack.write(data, 0, data.length);
                    mAudioTrack.play();
                }
            }
        } catch (Exception ex) {
            Log.d(TAG, Log.getStackTraceString(ex));
        } finally {
            close(fis);
            Log.d(TAG, "complete");
            if (mAudioTrack != null) {
                mAudioTrack.stop();
                mAudioTrack.release();
            }
            mState = State.NONE;
        }
    }

    private final class PlayThread extends Thread {
        @Override
        public void run() {
            handleTrackPlay(mUri);
        }
    }

    public void start() {
        synchronized (mLock) {
            mLock.notify();
        }
    }

    public void pause() {
        mState = State.PAUSE;
    }

    public void stop() {
        mState = State.STOP;
        if (this.mAudioTrack != null) {
            Log.d(TAG, "stop");
            mAudioTrack.stop();
            Log.d(TAG, "release");
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

    public State getState() {
        return mState;
    }
}
