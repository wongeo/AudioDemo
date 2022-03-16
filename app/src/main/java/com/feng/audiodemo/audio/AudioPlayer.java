package com.feng.audiodemo.audio;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import java.nio.ByteBuffer;

public class AudioPlayer implements IPlayer {

    private volatile AudioTrack mAudioTrack;
    private String mUri;
    private volatile boolean mIsRunning = false;
    private OnStateChangeListener mOnStateChangeListener;
    private final Object mLock = new Object();
    private final Context mContext;

    public AudioPlayer(Context context) {
        mContext = context;
    }

    @Override

    public void setDataSource(String uri) {
        mUri = uri;
    }

    @Override
    public void stop() {
        mIsRunning = false;
    }

    @Override
    public void setOnStateChangeListener(OnStateChangeListener listener) {
        mOnStateChangeListener = listener;
    }

    @Override
    public State getState() {
        return mState;
    }

    private volatile boolean mIsPause;

    @Override
    public void pause() {
        synchronized (mLock) {
            mIsPause = true;
        }
    }

    @Override
    public void prepare() {
        new Thread(() -> onPrepare()).start();
    }

    @Override
    public void start() {
        synchronized (mLock) {
            if (mIsPause) {
                mIsPause = false;
                mLock.notify();
            } else {
                prepare();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void onPrepare() {
        MediaExtractor extractor = new MediaExtractor();
        MediaCodec audioCodec = null;
        try {
            extractor.setDataSource(mUri);
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                MediaFormat mediaFormat = extractor.getTrackFormat(i);
                String mimeType = mediaFormat.getString(MediaFormat.KEY_MIME);
                if (mimeType.startsWith("audio/")) {
                    extractor.selectTrack(i);
                    int channelConfig = mediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    int sampleRateInHz = mediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);

                    int bufferSizeInBytes = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, AudioFormat.ENCODING_PCM_16BIT);

                    mAudioTrack = new AudioTrack(
                            AudioManager.STREAM_MUSIC,
                            sampleRateInHz,
                            (channelConfig == 1 ? AudioFormat.CHANNEL_OUT_MONO : AudioFormat.CHANNEL_OUT_STEREO),
                            AudioFormat.ENCODING_PCM_16BIT,
                            bufferSizeInBytes,
                            AudioTrack.MODE_STREAM);
                    mAudioTrack.play();
                    //创建解码器
                    audioCodec = MediaCodec.createDecoderByType(mimeType);
                    audioCodec.configure(mediaFormat, null, null, 0);
                    break;
                }
            }
            audioCodec.start();
            mIsRunning = true;
            mIsPause = false;
            onStateChange(State.START);

            MediaCodec.BufferInfo decodeBufferInfo = new MediaCodec.BufferInfo();
            while (mIsRunning) {
                int inputIndex = audioCodec.dequeueInputBuffer(10_000);
                if (inputIndex < 0) {
                    mIsRunning = false;
                }
                ByteBuffer inputBuffer = audioCodec.getInputBuffer(inputIndex);
                inputBuffer.clear();
                int sampleSize = extractor.readSampleData(inputBuffer, 0);
                if (sampleSize > 0) {
                    audioCodec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                    extractor.advance();
                } else {
                    mIsRunning = false;
                }

                int outputIndex = audioCodec.dequeueOutputBuffer(decodeBufferInfo, 10_000);
                ByteBuffer outputBuffer;
                byte[] chunkPCM;
                while (outputIndex >= 0) {
                    outputBuffer = audioCodec.getOutputBuffer(outputIndex);
                    chunkPCM = new byte[decodeBufferInfo.size];
                    outputBuffer.get(chunkPCM);
                    outputBuffer.clear();
                    AudioTrack track = mAudioTrack;
                    if (track != null) {
                        synchronized (mLock) {
                            if (mIsPause) {
                                onStateChange(State.PAUSE);
                                mLock.wait();
                                onStateChange(State.START);
                            } else if (!mIsRunning) {
                                onStateChange(State.STOP);
                                break;
                            }
                        }
                        track.write(chunkPCM, 0, decodeBufferInfo.size);
                        audioCodec.releaseOutputBuffer(outputIndex, false);
                        outputIndex = audioCodec.dequeueOutputBuffer(decodeBufferInfo, 10_000);
                    }

                }
            }
            Log.d(TAG, "complete");
        } catch (Exception e) {
            String msg = Log.getStackTraceString(e);
            Log.d(TAG, msg);
            onStateChange(State.ERROR);
            mOnErrorListener.onError(-1, msg);
        }
        audioCodec.stop();
        audioCodec.release();
        extractor.release();
    }

    private State mState = State.NONE;
    public static final String TAG = "AudioPlayer";

    /**
     * 播放状态切换回调
     */
    private void onStateChange(State state) {
        State from = mState;
        mState = state;
        Log.d(TAG, "onStateChange from=" + from + " to=" + state);
        mOnStateChangeListener.onChange(from, state);
    }

    private OnErrorListener mOnErrorListener;

    @Override
    public void setOnErrorListener(OnErrorListener onErrorListener) {
        mOnErrorListener = onErrorListener;
    }
}