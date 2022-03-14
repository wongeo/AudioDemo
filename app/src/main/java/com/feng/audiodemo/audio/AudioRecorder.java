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

        int sampleRateInHz = 44100;
        int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
        int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

        // 获得缓冲区字节大小
        //缓冲区大小，通过 AudioTrack.getMinBufferSize 运算得出
        int bufferSizeInBytes = AudioRecord.getMinBufferSize(
                sampleRateInHz,
                channelConfig,
                audioFormat);

        mAudioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRateInHz,
                channelConfig,
                audioFormat,
                bufferSizeInBytes);

        Log.d(TAG, "===startRecord===" + mAudioRecord.getState());
        mAudioRecord.startRecording();

        File file = new File(mFilePath);
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream fos = null;
        try {
            byte[] data = new byte[bufferSizeInBytes];

            fos = new FileOutputStream(file);
            while (mAudioRecord.read(data, 0, data.length) > 0) {
                if (mState == State.STOP) {
                    break;
                }
                mState = State.START;
                fos.write(data, 0, data.length);
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