package com.feng.audiodemo;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.audiofx.Visualizer;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.feng.audiodemo.adapter.FileAdapter;
import com.feng.audiodemo.adapter.Item;
import com.feng.audiodemo.audio.AudioRecorder;
import com.feng.audiodemo.audio.AudioPlayer;
import com.feng.audiodemo.audio.IPlayer;
import com.feng.audiodemo.view.AudioView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private AudioRecorder mAudioRecorder;

    private Button mRecordButton, mPlaylistButton, mPlayButton, mTranscodeButton;
    private TextView mTextView;
    private AudioPlayer mTrackPlayer;
    private IPlayer mPlayer;
    private AudioView audioView, audioView2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        verifyPermissions(this);
        initView();
        initPlayer();
    }

    private void initView() {
        mTextView = findViewById(R.id.log_txt);
        mRecordButton = findViewById(R.id.record_btn);
        mPlaylistButton = findViewById(R.id.play_list_btn);
        mPlayButton = findViewById(R.id.play_btn);
        mTranscodeButton = findViewById(R.id.transcode_btn);
        audioView = findViewById(R.id.audioView1);
        audioView2 = findViewById(R.id.audioView2);

        mRecordButton.setOnClickListener(this);
        mPlaylistButton.setOnClickListener(this);
        mPlayButton.setOnClickListener(this);
        mTranscodeButton.setOnClickListener(this);
    }

    private void initPlayer() {
        mPlayer = new AudioPlayer(this);
        mPlayer.setOnStateChangeListener(mOnStateChangeListener);
        mPlayer.setOnErrorListener(mOnErrorListener);
    }

    @Override
    public void onClick(View v) {
        if (v == mRecordButton) {
            startRecord();
        } else if (v == mPlaylistButton) {
            showList(this);
        } else if (v == mPlayButton) {
            onStartOrPause();
        } else if (mTranscodeButton == v) {
            onTranscodeButton();
        }
    }

    private Visualizer.OnDataCaptureListener dataCaptureListener = new Visualizer.OnDataCaptureListener() {
        @Override
        public void onWaveFormDataCapture(Visualizer visualizer, final byte[] waveform, int samplingRate) {
            audioView.post(new Runnable() {
                @Override
                public void run() {
                    audioView.setWaveData(waveform);
                }
            });
        }

        @Override
        public void onFftDataCapture(Visualizer visualizer, final byte[] fft, int samplingRate) {
            audioView2.post(new Runnable() {
                @Override
                public void run() {
                    audioView2.setWaveData(fft);
                }
            });
        }
    };

    private Visualizer visualizer;

    private void initVisualizer() {
        try {
            int mediaPlayerId = mTrackPlayer.getMediaPlayerId();
            if (visualizer != null) {
                visualizer.release();
            }
            visualizer = new Visualizer(mediaPlayerId);

            /**
             *可视化数据的大小： getCaptureSizeRange()[0]为最小值，getCaptureSizeRange()[1]为最大值
             */
            int captureSize = Visualizer.getCaptureSizeRange()[1];
            int captureRate = Visualizer.getMaxCaptureRate() * 3 / 4;

            visualizer.setCaptureSize(captureSize);
            visualizer.setDataCaptureListener(dataCaptureListener, captureRate, true, true);
            visualizer.setScalingMode(Visualizer.SCALING_MODE_NORMALIZED);
            visualizer.setEnabled(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onTranscodeButton() {
//        TransPCMHandlerPure pcmHandlerPure = new TransPCMHandlerPure(src, desc);
//        pcmHandlerPure.start();
        String src = this.getExternalFilesDir(null).getAbsolutePath() + "/abc.mp3";
        String desc = this.getExternalFilesDir(null).getAbsolutePath() + "/abc.wav";
        File file = new File(desc);
        if (file.exists()) {
            file.delete();
        }
//        codec.start();
    }

    /**
     * 开始录音|停止录音
     */
    private void startRecord() {
        @SuppressLint("SimpleDateFormat") String fileName = new SimpleDateFormat("yyyyMMdd_hh:mm:ss").format(new Date());
        if (mAudioRecorder == null) {
            mAudioRecorder = new AudioRecorder(this);
            String filePath = getExternalFilesDir(null) + "/" + fileName + ".raw";
            mAudioRecorder.setFilePath(filePath);
            mAudioRecorder.startRecord();
            mRecordButton.setText("停止录音");
        } else {
            mAudioRecorder.release();
            mAudioRecorder = null;
            mRecordButton.setText("开始录音");
        }
    }

    /**
     * 显示所有录音文件
     */
    @RequiresApi(api = Build.VERSION_CODES.N)
    private void showList(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, 0);
        builder.setTitle("视频列表");
        CompletableFuture<List<Item>> future = CompletableFuture.supplyAsync(() -> {
            //读取存储卡跟目录mp4视频
            File dir = context.getExternalFilesDir(null);
            File[] files = dir.listFiles();

            List<Item> items = new ArrayList<>();
            if (files != null) {
                for (File file : files) {
                    items.add(new Item(file.getName(), file.getAbsolutePath()));
                }
            }
            return items;
        }).exceptionally(throwable -> {
            //
            //
            return null;
        });

        future.whenComplete((items, throwable) -> runOnUiThread(() -> {
            builder.setAdapter(new FileAdapter(context, items), (dialog, which) -> {
                dialog.dismiss();
                Item item = items.get(which);
                String uri = item.getUri();
                onPlay(uri);
            });
            builder.create().show();
        })).exceptionally(throwable -> {
            //fff
            //fff
            return null;
        });
    }

    public void onStartOrPause() {
        IPlayer.State state = mPlayer.getState();
        if (state == IPlayer.State.START) {
            mPlayer.pause();
        } else if (state == IPlayer.State.PAUSE) {
            mPlayer.start();
        }
    }

    private void onPlay(String uri) {
//        mTrackPlayer.stop();
//        mTrackPlayer.setDataSource(uri);
//        mTrackPlayer.prepare();
        mPlayer.stop();
        mPlayer.setDataSource(uri);
        mPlayer.prepare();
    }


    private final IPlayer.OnStateChangeListener mOnStateChangeListener = (src, desc) -> runOnUiThread(() -> handleStateChangeOnUiThread(src, desc));

    private void handleStateChangeOnUiThread(IPlayer.State src, IPlayer.State desc) {
        if (desc == IPlayer.State.START) {
            mPlayButton.setText("暂停");
            initVisualizer();
        } else if (desc == IPlayer.State.PAUSE) {
            mPlayButton.setText("播放");
        }
    }

    private final IPlayer.OnErrorListener mOnErrorListener = (code, msg) -> runOnUiThread(() -> handleError(code, msg));

    private void handleError(int code, String msg) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTrackPlayer.stop();
        mPlayer.stop();
    }

    private static final String[] sPermissions = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.MODIFY_AUDIO_SETTINGS
    };

    /**
     * 申请录音权限
     */
    public static void verifyPermissions(Activity activity) {
        for (String permission : sPermissions) {
            boolean hasRequest = ActivityCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED;
            if (hasRequest) {
                ActivityCompat.requestPermissions(activity, sPermissions, 1);
                break;
            }
        }
    }
}