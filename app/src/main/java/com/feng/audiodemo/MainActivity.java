package com.feng.audiodemo;

import androidx.annotation.RequiresApi;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.feng.audiodemo.adapter.FileAdapter;
import com.feng.audiodemo.adapter.Item;
import com.feng.audiodemo.audio.AudioRecorder;
import com.feng.audiodemo.audio.AudioTrackPlayer;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private AudioRecorder mAudioRecorder;

    private Button mRecordButton, mPlayListButton, mPlayButton;

    private AudioTrackPlayer mTrackPlayer;
    private String mUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        verifyPermissions(this);
    }

    private void initView() {
        mRecordButton = findViewById(R.id.record_btn);
        mPlayListButton = findViewById(R.id.play_list_btn);
        mPlayButton = findViewById(R.id.play_btn);

        mRecordButton.setOnClickListener(this);
        mPlayListButton.setOnClickListener(this);
        mPlayButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == mRecordButton) {
            startRecord();
        } else if (v == mPlayListButton) {
            showList(this);
        } else if (v == mPlayButton) {
            onPlay();
        }
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
                mUri = item.getUri();
                onPlay();
            });
            builder.create().show();
        })).exceptionally(throwable -> {
            //fff
            //fff
            return null;
        });
    }

    private void onPlay() {
        if (mTrackPlayer == null) {
            mTrackPlayer = new AudioTrackPlayer(this);
        }
        AudioTrackPlayer.State state = mTrackPlayer.getState();
        if (state == AudioTrackPlayer.State.START) {
            mTrackPlayer.pause();
            mPlayButton.setText("播放");
        } else if (state == AudioTrackPlayer.State.PAUSE) {
            mTrackPlayer.start();
            mPlayButton.setText("暂停");
        } else {
            mTrackPlayer.setDataSource(mUri);
            mTrackPlayer.prepare();
            mPlayButton.setText("暂停");
        }
    }

    private static final String[] sPermissions = {
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
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