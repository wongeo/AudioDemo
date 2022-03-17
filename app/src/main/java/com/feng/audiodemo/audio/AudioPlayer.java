package com.feng.audiodemo.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.io.IOException;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class AudioPlayer implements IPlayer {
    public static final String TAG = "AudioPlayer";

    private Context mContext;
    private AudioTrack mAudioTrack;
    private Thread mPlayThread;

    public int getMediaPlayerId() {
        return mAudioTrack.getAudioSessionId();
    }

    private volatile State mState = State.NONE;

    public AudioPlayer(Context context) {
        mContext = context;
    }

    private OnStateChangeListener mOnStateChangeListener;
    private OnErrorListener mOnErrorListener;

    public void setOnStateChangeListener(OnStateChangeListener listener) {
        mOnStateChangeListener = listener;
    }

    public void setOnErrorListener(OnErrorListener onErrorListener) {
        mOnErrorListener = onErrorListener;
    }

    public static final int RATE_IN_HZ_16K = 16000;
    public static final int RATE_IN_HZ_44K = 44100;

    private String mUri;

    /**
     * 设置播放地址
     */
    @Override
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

        int sampleRateInHz = RATE_IN_HZ_44K;
        int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

        ISource fis = null;
        try {
            if (uri.endsWith(".pcm") || uri.endsWith(".wav")) {
                fis = new FileSource(uri);
            } else {
                fis = new FileSourceWithCodec(uri);
            }
            MediaFormat format = fis.getMediaFormat();
            sampleRateInHz = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);

            int bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
            if (bufferSizeInBytes == AudioTrack.ERROR_BAD_VALUE) {
                Log.e(TAG, "Invalid parameter !");
                return;
            }
            Log.i(TAG, "bufferSizeInBytes = " + bufferSizeInBytes + " bytes !");
            byte[] data = new byte[bufferSizeInBytes];
            mAudioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,  //streamType 流类型，AudioManager 中定义了音频的类型，可大致分为 STREAM_MUSIC 、 STREAM_RINHG 等
                    sampleRateInHz,             //sampleRateInHz 采样率，播放的音频每秒有多少次采样
                    channelConfig,              //channelConfig 声道数配置，单声道和双声道
                    audioFormat,                //audioFormat 数据位宽，选择 16bit ，能够兼容所有 Android 设备
                    bufferSizeInBytes,          //bufferSizeInBytes 缓冲区大小，通过 AudioTrack.getMinBufferSize 运算得出
                    AudioTrack.MODE_STREAM      //mode 播放模式 ： MODE_STATIC 一次写入，MODE_STREAM 多次写入
            );

            if (mAudioTrack.getState() == AudioTrack.STATE_UNINITIALIZED) {
                Log.e(TAG, "初始化失败 !");
                return;
            }
            Log.d(TAG, "开始填充数据...");

            onStateChange(State.START);
            mIsRunning = true;
            mIsPause = false;
            int len;
            while ((len = fis.read(data)) > 0) {
                synchronized (mLock) {
                    if (mIsPause) {
                        onStateChange(State.PAUSE);
                        mLock.wait();
                        onStateChange(State.START);
                    } else if (!mIsRunning) {
                        onStateChange(State.STOP);
                        break;
                    }
                    mAudioTrack.write(data, 0, len);
                    mAudioTrack.play();
                }
            }
            Log.d(TAG, "complete");
            onStateChange(State.COMPLETED);
        } catch (Exception ex) {
            String msg = Log.getStackTraceString(ex);
            Log.d(TAG, msg);
            onStateChange(State.ERROR);
            mOnErrorListener.onError(-1, msg);
        } finally {
            ISource.close(fis);
            reset();
        }
    }

    private final class PlayThread extends Thread {
        @Override
        public void run() {
            handleTrackPlay(mUri);
        }
    }

    @Override
    public void start() {
        synchronized (mLock) {
            mIsPause = false;
            mLock.notify();
        }
    }

    @Override
    public void pause() {
        synchronized (mLock) {
            if (mState == State.START) {
                mIsPause = true;
            }
        }
    }

    public void reset() {
        synchronized (mLock) {
            if (mAudioTrack != null) {
                mAudioTrack.stop();
                mAudioTrack.release();
            }
        }
    }

    //控制变量，用来控制停止和暂停
    private boolean mIsPause;
    private boolean mIsRunning;

    public void stop() {
        synchronized (mLock) {
            mIsRunning = false;
        }
    }

    public State getState() {
        return mState;
    }

    /**
     * 播放状态切换回调
     */
    private void onStateChange(State state) {
        State from = mState;
        mState = state;
        Log.d(TAG, "onStateChange from=" + from + " to=" + state);
        mOnStateChangeListener.onChange(from, state);
    }
}
