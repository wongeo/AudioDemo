package com.feng.audiodemo.audio;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.text.TextUtils;
import android.util.Log;

import java.nio.ByteBuffer;

/**
 * 解码
 */
public class AudioCodec {
    private static final long TIME_OUT = 5000;
    public static final String TAG = "AudioCodec";
    private final String mFilePath;
    private MediaCodec mMediaCodec;
    private MediaExtractor mExtractor;
    private long mDurationUs;

    private int rangeStart, rangeEnd;

    private AudioCodec(String filePath) {
        mFilePath = filePath;

    }

    public static AudioCodec create(String filePath) {
        AudioCodec audioCodec = new AudioCodec(filePath);
        audioCodec.init();
        return audioCodec;
    }

    /**
     * 初始化解码器
     */
    private void init() {
        mExtractor = new MediaExtractor();
        try {
            mExtractor.setDataSource(mFilePath);
            int numTracks = mExtractor.getTrackCount();
            for (int i = 0; i < numTracks; i++) {
                MediaFormat format = mExtractor.getTrackFormat(i);
                String mine = format.getString(MediaFormat.KEY_MIME);
                if (!TextUtils.isEmpty(mine) && mine.startsWith("audio")) {//获取音频轨道
                    mExtractor.selectTrack(i);//选择此音频轨道
                    mDurationUs = format.getLong(MediaFormat.KEY_DURATION);
                    //创建Decode解码器
                    mMediaCodec = MediaCodec.createDecoderByType(mine);
                    mMediaCodec.configure(format, null, null, 0);
                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        mMediaCodec.start();//启动MediaCodec ，等待传入数据

        decodeInputBuffers = mMediaCodec.getInputBuffers();//MediaCodec在此ByteBuffer[]中获取输入数据
        decodeOutputBuffers = mMediaCodec.getOutputBuffers();//MediaCodec将解码后的数据放到此ByteBuffer[]中 我们可以直接在这里面得到PCM数据
        decodeBufferInfo = new MediaCodec.BufferInfo();
    }

    private ByteBuffer[] decodeInputBuffers;
    private ByteBuffer[] decodeOutputBuffers;
    private MediaCodec.BufferInfo decodeBufferInfo;

    public static class Data {
        public byte[] bytes;
        public int code;
    }


    public Data read() {
        long positionUs = 0;
        int inputIndex = mMediaCodec.dequeueInputBuffer(-1);//获取可用的inputBuffer -1代表一直等待，0表示不等待 建议-1,避免丢帧
        if (inputIndex >= 0) {
            ByteBuffer inputBuffer = decodeInputBuffers[inputIndex];//拿到inputBuffer
            inputBuffer.clear();//清空之前传入inputBuffer内的数据
            int sampleSize = mExtractor.readSampleData(inputBuffer, 0);//MediaExtractor读取数据到inputBuffer中
            positionUs = mExtractor.getSampleTime();
            if (sampleSize < 0) {
                //小于0 代表所有数据已读取完成
            } else {
                mMediaCodec.queueInputBuffer(inputIndex, 0, sampleSize, 0, 0);//通知MediaDecode解码刚刚传入的数据
                mExtractor.advance();//MediaExtractor移动到下一取样处
            }
        }

        //获取解码得到的byte[]数据 参数BufferInfo上面已介绍 10000同样为等待时间 同上-1代表一直等待，0代表不等待。此处单位为微秒
        //此处建议不要填-1 有些时候并没有数据输出，那么他就会一直卡在这 等待
        int outputIndex = mMediaCodec.dequeueOutputBuffer(decodeBufferInfo, 10000);
        ByteBuffer outputBuffer;
        Data data = new Data();
        switch (outputIndex) {
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                data.code = outputIndex;
                break;
            default:
                data.code = 0;
                outputBuffer = decodeOutputBuffers[outputIndex];//拿到用于存放PCM数据的Buffer
                data.bytes = new byte[decodeBufferInfo.size];
                outputBuffer.get(data.bytes, 0, data.bytes.length);
                mMediaCodec.releaseOutputBuffer(outputIndex, true);
                Log.d(TAG, "positionUs=" + positionUs + " durationUs=" + mDurationUs + " outputIndex=" + outputIndex + " len=" + data.bytes.length);
                break;
        }

        return data;
    }

    public void stop() {
        mMediaCodec.stop();
    }

    public long getDurationMs() {
        return mDurationUs / 1000;
    }
}
