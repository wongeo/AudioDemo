package com.feng.audiodemo.audio;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    private final static int AUDIO_SAMPLE_RATE = 16000;
    //声道 单声道
    private final static int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    //编码
    private final static int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    // 缓冲区字节大小
    private int bufferSizeInBytes = 0;

    //录音对象
    private AudioRecord mAudioRecord = new AudioRecord(AUDIO_INPUT, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING, bufferSizeInBytes);

    //录音状态
    private State mState = State.NONE;

    //文件名
    private String mFileName;

    private final Context mContext;

    public AudioRecorder(Context context) {
        mContext = context;
    }

    /**
     * 创建默认的录音对象
     *
     * @param fileName 文件名
     */
    @RequiresPermission(android.Manifest.permission.RECORD_AUDIO)
    public void createDefaultAudio(String fileName) {
        mFileName = fileName;
        // 获得缓冲区字节大小
        bufferSizeInBytes = AudioRecord.getMinBufferSize(
                AUDIO_SAMPLE_RATE,
                AUDIO_CHANNEL,
                AUDIO_ENCODING);
        mAudioRecord = new AudioRecord(AUDIO_INPUT, AUDIO_SAMPLE_RATE, AUDIO_CHANNEL, AUDIO_ENCODING, bufferSizeInBytes);
        mFileName = fileName;
        mState = State.READY;
    }


    /**
     * 开始录音
     */
    public void startRecord() {

        if (mState == State.NONE || TextUtils.isEmpty(mFileName)) {
            throw new IllegalStateException("录音尚未初始化,请检查是否禁止了录音权限~");
        }
        if (mState == State.START) {
            throw new IllegalStateException("正在录音");
        }
        Log.d(TAG, "===startRecord===" + mAudioRecord.getState());
        mAudioRecord.startRecording();

        new Thread(new Runnable() {
            @Override
            public void run() {
                writeDataTOFile();
            }
        }).start();
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
        Log.d(TAG, "===stopRecord===");
        if (mState == State.NONE || mState == State.READY) {
            throw new IllegalStateException("录音尚未开始");
        } else {
            mAudioRecord.stop();
            mState = State.STOP;
            release();
        }
    }

    /**
     * 释放资源
     */
    public void release() {
        Log.d(TAG, "===release===");
        //假如有暂停录音
//        try {
//            if (filesName.size() > 0) {
//                //清除
//                filesName.clear();
//                //将多个pcm文件转化为wav文件
////                mergePCMFilesToWAVFile(filePaths);
//            } else {
//                //这里由于只要录音过filesName.size都会大于0,没录音时fileName为null
//                //会报空指针 NullPointerException
//                // 将单个pcm文件转化为wav文件
//                //Log.d(TAG, "=====makePCMFileToWAVFile======");
//                //makePCMFileToWAVFile();
//            }
//        } catch (IllegalStateException e) {
//            throw new IllegalStateException(e.getMessage());
//        }

        if (mAudioRecord != null) {
            mAudioRecord.release();
            mAudioRecord = null;
        }

        mState = State.NONE;
    }

    /**
     * 取消录音
     */
    public void cancel() {
//        filesName.clear();
//        fileName = null;
        if (mAudioRecord != null) {
            mAudioRecord.release();
            mAudioRecord = null;
        }

        mState = State.NONE;
    }


    /**
     * 将音频信息写入文件
     */
    private void writeDataTOFile() {
        // new一个byte数组用来存一些字节数据，大小为缓冲区大小
        byte[] audioData = new byte[bufferSizeInBytes];

        FileOutputStream fos = null;

        try {
            String currentFileName = mFileName;
            if (mState == State.PAUSE) {
                //假如是暂停录音 将文件名后面加个数字,防止重名文件内容被覆盖
//                currentFileName += filesName.size();
            }
//            filesName.add(currentFileName);

            File file = new File(mContext.getExternalFilesDir(null).getAbsolutePath(), currentFileName);
            if (file.exists()) {
                file.delete();
            }
            fos = new FileOutputStream(file);// 建立一个可存取字节的文件
        } catch (IllegalStateException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            throw new IllegalStateException(e.getMessage());
        } catch (FileNotFoundException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        //将录音状态设置成正在录音状态
        mState = State.START;
        int len = 0;
        while (mState == State.START) {
            len = mAudioRecord.read(audioData, 0, bufferSizeInBytes);
            if (AudioRecord.ERROR_INVALID_OPERATION != len && fos != null) {
                try {
                    fos.write(audioData);
                } catch (IOException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
            }
        }
        try {
            if (fos != null) {
                fos.close();// 关闭写入流
            }
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
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
        //预备
        READY,
        //录音
        START,
        //暂停
        PAUSE,
        //停止
        STOP
    }

}