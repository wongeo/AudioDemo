package com.feng.audiodemo.audio;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.text.TextUtils;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;

/**
 * 实现录音
 */
public class AudioRecorder {
    public static final String TAG = "AudioRecorder";
    //音频输入-麦克风
    private final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;
    //采用频率
    //44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    //采样频率一般共分为22.05KHz、44.1KHz、48KHz三个等级
    //采样率，播放的音频每秒有多少次采样
    private final static int AUDIO_SAMPLE_RATE = 48000;
    //声道 单声道
    private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    //编码
    //数据位宽，选择 16bit ，能够兼容所有 Android 设备
    private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    // 缓冲区字节大小
    //缓冲区大小，通过 AudioTrack.getMinBufferSize 运算得出
    private int bufferSizeInBytes = 0;

    //录音对象
    private AudioRecord mAudioRecord;

    //录音状态
    private State mState = State.NONE;

    //文件路径
    private String mFilePath;

    private final Context mContext;

    public AudioRecorder(Context context) {
        mContext = context;
    }

    /**
     * 创建默认的录音对象
     */
    public void setFilePath(String filePath) {
        mFilePath = filePath;
    }


    private final class RecordThread extends Thread {
        @Override
        public void run() {
            handleRecord();
        }
    }

    @SuppressLint("MissingPermission")
    private void handleRecord() {
        if (TextUtils.isEmpty(mFilePath)) {
            throw new IllegalStateException("录音尚未初始化,请检查是否禁止了录音权限~");
        }
        // 获得缓冲区字节大小
        bufferSizeInBytes = AudioRecord.getMinBufferSize(
                AUDIO_SAMPLE_RATE,
                AUDIO_CHANNEL,
                AUDIO_ENCODING);
        mAudioRecord = new AudioRecord(
                AUDIO_INPUT,
                AUDIO_SAMPLE_RATE,
                AUDIO_CHANNEL,
                AUDIO_ENCODING,
                bufferSizeInBytes);

        Log.d(TAG, "===startRecord===" + mAudioRecord.getState());
        mAudioRecord.startRecording();

        // new一个byte数组用来存一些字节数据，大小为缓冲区大小
        byte[] audioData = new byte[bufferSizeInBytes];
        File file = new File(mFilePath);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(file);// 建立一个可存取字节的文件
            while (mAudioRecord.read(audioData, 0, audioData.length) > 0) {
                if (mState == State.PAUSE) {
                    Log.d(TAG, "pause");
//                    mLock.wait();
                    Log.d(TAG, "start");
                } else if (mState == State.STOP) {
                    break;
                }
                mState = State.START;
                fos.write(audioData, 0, audioData.length);
            }
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            close(fos);
            if (mAudioRecord != null) {
                mAudioRecord.release();
                mAudioRecord = null;
            }
            mState = State.NONE;
        }
    }

    /**
     * 开始录音
     */
    public void startRecord() {
        Thread recordThread = new RecordThread();
        recordThread.start();
    }

    /**
     * 暂停录音
     */
    public void pauseRecord() {
        Log.d(TAG, "===pauseRecord===");
        if (mState != State.START) {
            throw new IllegalStateException("没有在录音");
        } else {
            mAudioRecord.stop();
            mState = State.PAUSE;
        }
    }

    /**
     * 停止录音
     */
    public void stopRecord() {
        mState = State.STOP;
    }

    /**
     * 释放资源
     */
    public void release() {
        mState = State.STOP;
    }

    /**
     * 取消录音
     */
    public void cancel() {
        mState = State.STOP;
    }


    /**
     * 将音频信息写入文件
     */
    private void writeDataTOFile(String filePath) {

    }

    /**
     * 获取录音对象的状态
     *
     * @return
     */
    public State getState() {
        return mState;
    }

    /**
     * 录音对象的状态
     */
    public enum State {
        //未开始
        NONE,
        //录音
        START,
        //暂停
        PAUSE,
        //停止
        STOP
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