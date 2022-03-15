package com.feng.audiodemo.audio;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 解码
 */
public class AudioCodec {
    private static final long TIME_OUT = 5000;
    public static final String TAG = "AudioCodec";
    private final String mFilePath;
    private MediaCodec codec;
    private MediaExtractor extractor;
    private long mDurationUs;

    private int rangeStart, rangeEnd;

    private AudioCodec(String filePath) {
        mFilePath = filePath;

    }

    public static AudioCodec create(String filePath) throws IOException {
        AudioCodec audioCodec = new AudioCodec(filePath);
        audioCodec.initDecoder();
        return audioCodec;
    }

    /**
     * 初始化解码器
     */
    private void initDecoder() throws IOException {
        extractor = new MediaExtractor();
        extractor.setDataSource(mFilePath);
        int numTracks = extractor.getTrackCount();
        for (int i = 0; i < numTracks; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String mine = format.getString(MediaFormat.KEY_MIME);
            if (!TextUtils.isEmpty(mine) && mine.startsWith("audio")) {//获取音频轨道
                extractor.selectTrack(i);//选择此音频轨道
                mDurationUs = format.getLong(MediaFormat.KEY_DURATION);
                //创建Decode解码器
                codec = MediaCodec.createDecoderByType(mine);
                codec.configure(format, null, null, 0);
                break;
            }
        }

        codec.start();//启动MediaCodec ，等待传入数据
        decodeBufferInfo = new MediaCodec.BufferInfo();
    }

    private MediaCodec.BufferInfo decodeBufferInfo;


    public Data read() {
        Data data = new Data();
        data.durationUs = mDurationUs;
        int inputBufferIndex = codec.dequeueInputBuffer(-1);//获取可用的inputBuffer -1代表一直等待，0表示不等待 建议-1,避免丢帧
        if (inputBufferIndex >= 0) {
            ByteBuffer inputBuffer = codec.getInputBuffer(inputBufferIndex);//拿到inputBuffer
            inputBuffer.clear();//清空之前传入inputBuffer内的数据
            int sampleSize = extractor.readSampleData(inputBuffer, 0);//MediaExtractor读取数据到inputBuffer中
            data.positionUs = extractor.getSampleTime();
            if (sampleSize < 0) {
                //小于0 代表所有数据已读取完成
                data.code = -1;
                return data;
            }
            codec.queueInputBuffer(inputBufferIndex, 0, sampleSize, 0, 0);//通知MediaDecode解码刚刚传入的数据
            extractor.advance();//MediaExtractor移动到下一取样处
        }

        //获取解码得到的byte[]数据 参数BufferInfo上面已介绍 10000同样为等待时间 同上-1代表一直等待，0代表不等待。此处单位为微秒
        //此处建议不要填-1 有些时候并没有数据输出，那么他就会一直卡在这 等待
        int outputBufferIndex = codec.dequeueOutputBuffer(decodeBufferInfo, 10000);

        switch (outputBufferIndex) {
            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
            case MediaCodec.INFO_TRY_AGAIN_LATER:
                data.code = outputBufferIndex;
                break;
            default:
                data.code = 0;
                ByteBuffer outputBuffer = codec.getOutputBuffer(outputBufferIndex);//拿到用于存放PCM数据的Buffer
                data.bytes = new byte[decodeBufferInfo.size];
                outputBuffer.get(data.bytes, 0, data.bytes.length);
                codec.releaseOutputBuffer(outputBufferIndex, true);
                Log.d(TAG, data.toString());
                break;
        }
        return data;
    }

    public void stop() {
        codec.stop();
    }

    public long getDurationMs() {
        return mDurationUs / 1000;
    }

    public static class Data {
        public long positionUs;
        public long durationUs;
        public byte[] bytes;
        /**
         * 0：接收字节，-1文件结束
         */
        public int code;

        @Override
        public String toString() {
            return "positionUs=" + positionUs + " durationUs=" + durationUs + " len=" + bytes.length;
        }
    }
}
