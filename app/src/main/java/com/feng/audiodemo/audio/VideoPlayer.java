package com.feng.audiodemo.audio;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.SystemClock;
import android.view.Surface;

import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoPlayer {
    private volatile boolean mIsPlaying = false;
    private Surface mSurface;
    private volatile boolean mEOF = false;
    private String mUri;

    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    public void setDataSource(String uri) {
        mUri = uri;
    }

    public void prepare() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                onStart();
            }
        }).start();
    }

    public void stop() {
        mIsPlaying = true;
    }

    private void onStart() {
        MediaExtractor videoExtractor = new MediaExtractor();
        MediaCodec videoCodec = null;
        long startWhen = 0;
        try {
            videoExtractor.setDataSource(mUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        boolean firstFrame = false;
        for (int i = 0; i < videoExtractor.getTrackCount(); i++) {
            MediaFormat mediaFormat = videoExtractor.getTrackFormat(i);
            String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
            if (mimeType.startsWith("video/")) {
                videoExtractor.selectTrack(i);
                try {
                    videoCodec = MediaCodec.createDecoderByType(mimeType);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                videoCodec.configure(mediaFormat, mSurface, null, 0);
                break;
            }
        }
        if (videoCodec == null) {
            return;
        }
        videoCodec.start();
        while (!mEOF) {
            MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
            ByteBuffer[] inputBuffers = videoCodec.getInputBuffers();
            int inputIndex = videoCodec.dequeueInputBuffer(10_000);
            if (inputIndex > 0) {
                ByteBuffer byteBuffer = inputBuffers[inputIndex];
                int sampleSize = videoExtractor.readSampleData(byteBuffer, 0);
                if (sampleSize > 0) {
                    videoCodec.queueInputBuffer(inputIndex, 0, sampleSize, videoExtractor.getSampleTime(), 0);
                    videoExtractor.advance();
                } else {
                    videoCodec.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                }
            }

            int outputIndex = videoCodec.dequeueOutputBuffer(bufferInfo, 10_000);

            if (outputIndex >= 0) {
                if (!firstFrame) {
                    startWhen = System.currentTimeMillis();
                    firstFrame = true;
                }
                long sleepTime = (bufferInfo.presentationTimeUs / 1000) - (System.currentTimeMillis() - startWhen);
                if (sleepTime > 0) {
                    SystemClock.sleep(sleepTime);
                }
                videoCodec.releaseOutputBuffer(outputIndex, true);
            }
            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                mEOF = true;
                break;
            }
        }

        videoCodec.stop();
        videoCodec.release();
        videoExtractor.release();
    }

}
