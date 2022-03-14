package com.feng.audiodemo.audio;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

@RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
public class TransPCMHandlerPure {
    private String srcFile;
    private String outFile;
    private long rangeStart = -1;
    private long rangeEnd = -1;
    private OnProgressListener listener;


    public TransPCMHandlerPure(String srcFile, String outFile) {
        this(srcFile, outFile, null);
    }

    public TransPCMHandlerPure(String srcFile, String outFile, OnProgressListener listener) {
        this(srcFile, outFile, -1, -1, listener);
    }

    public TransPCMHandlerPure(String srcFile, String outFile, long rangeStart, long rangeEnd, OnProgressListener listener) {
        this.srcFile = srcFile;
        this.outFile = outFile;
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
        this.listener = listener;
    }

    public void start() {
        DecodeTask task = new DecodeTask(srcFile, outFile, listener);
        task.setRangeTime(rangeStart, rangeEnd);
        new Thread(task).start();
    }

    public void setRangeTime(long rangeStart, long rangeEnd) {
        this.rangeStart = rangeStart;
        this.rangeEnd = rangeEnd;
    }


    public void setListener(OnProgressListener listener) {
        this.listener = listener;
    }

    private static class DecodeTask implements Runnable{
        private static final long TIME_OUT = 5000;
        private MediaExtractor extractor;
        private String srcFile;
        private MediaCodec codec;
        private String outFile;
        private OnProgressListener listener;
        private long rangeStart;
        private long rangeEnd;
        private int duration = 0;
        private OutputStream mOutput;

        public void setRangeTime(long rangeStart, long rangeEnd) {
            this.rangeStart = rangeStart;
            this.rangeEnd = rangeEnd;
        }

        public DecodeTask(String srcFile, String outFile, OnProgressListener listener) {
            this.srcFile = srcFile;
            this.outFile = outFile;
            this.listener = listener;
        }

        @Override
        public void run() {
            if (listener != null) {
                listener.onStart();
            }
            boolean isPrepare = false;
            try {
                prepare();
                isPrepare = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (isPrepare) {
                output();
            }
            release();
            if (!isPrepare && listener != null) {
                listener.onFail();
            }
        }

        private void release() {
            if (extractor != null) {
                extractor.release();
                extractor = null;
            }
            if (codec != null) {
                codec.stop();
                codec.release();
                codec = null;
            }
        }


        private void prepare() throws IOException {
            extractor = new MediaExtractor();
            extractor.setDataSource(srcFile);
            int numTracks = extractor.getTrackCount();
            for (int i = 0; i < numTracks; i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mine = format.getString(MediaFormat.KEY_MIME);
                if (!TextUtils.isEmpty(mine) && mine.startsWith("audio")) {
                    extractor.selectTrack(i);
                    try {
                        duration = format.getInteger(MediaFormat.KEY_DURATION) / 1000;
                    } catch (Exception e) {
                        e.printStackTrace();
                        MediaPlayer mediaPlayer = new MediaPlayer();
                        mediaPlayer.setDataSource(srcFile);
                        mediaPlayer.prepare();
                        duration = mediaPlayer.getDuration();
                        mediaPlayer.release();
                    }
                    codec = MediaCodec.createDecoderByType(mine);
                    codec.configure(format, null, null, 0);
                    codec.start();
                    break;
                }
            }
            createFile(outFile + ".pcm", true);
            mOutput = new DataOutputStream(new FileOutputStream(outFile + ".pcm"));
        }

        private void  output(){
            ByteBuffer[] inputBuffers = codec.getInputBuffers();
            ByteBuffer[] outputBuffers = codec.getOutputBuffers();
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            if (rangeStart > 0) {//如果有裁剪，seek到裁剪的地方
                extractor.seekTo(rangeStart * 1000, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            }
            boolean isEOS = false;
            while (true){
                long timestamp = 0;
                if (!isEOS) {
                    int inIndex = codec.dequeueInputBuffer(TIME_OUT);
                    if (inIndex >= 0) {
                        ByteBuffer buffer = inputBuffers[inIndex];
                        int sampleSize = extractor.readSampleData(buffer, 0);
                        long timestampTemp = extractor.getSampleTime();
                        timestamp = timestampTemp / 1000;
                        if (rangeEnd > 0 && timestamp > rangeEnd) {
                            sampleSize = -1;
                        }
                        if (sampleSize <= 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            isEOS = true;
                        } else {
                            codec.queueInputBuffer(inIndex, 0, sampleSize, timestampTemp, 0);
                            extractor.advance();
                        }
                    }
                }
                int outIndex = codec.dequeueOutputBuffer(info, TIME_OUT);
                switch (outIndex) {
                    case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                        outputBuffers = codec.getOutputBuffers();
                        break;
                    case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    case MediaCodec.INFO_TRY_AGAIN_LATER:
                        break;
                    default:
                        ByteBuffer buffer = outputBuffers[outIndex];
                        byte[] outData = new byte[info.size];
                        buffer.get(outData, 0, info.size);
                        codec.releaseOutputBuffer(outIndex, true);
                        try {
                            mOutput.write(outData);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        if (listener != null) {
                            listener.onProgress(rangeEnd > 0 ? (int) rangeEnd : duration, rangeStart > 0 ? (int) (timestamp - rangeStart) : (int) timestamp);
                        }
                        break;
                }
            }
        }
    }

    private static boolean createFile(String filePath, boolean recreate) {
        if (TextUtils.isEmpty(filePath)) {
            return false;
        }
        try {
            File file = new File(filePath);
            if (file.exists()) {
                if (recreate) {
                    file.delete();
                    file.createNewFile();
                }
            } else {
                // 如果路径不存在，先创建路径
                File parentFile = file.getParentFile();
                if (!parentFile.exists()) {
                    parentFile.mkdirs();
                }
                file.createNewFile();
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public interface OnProgressListener {
        void onStart();

        void onProgress(int max, int progress);

        void onSuccess();

        void onFail();
    }

}

