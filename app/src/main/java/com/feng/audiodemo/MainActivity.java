package com.feng.audiodemo;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.feng.audiodemo.adapter.FileAdapter;
import com.feng.audiodemo.adapter.Item;
import com.feng.audiodemo.record.AudioRecorder;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {


    private AudioRecorder mAudioRecorder;

    private Button mRecordButton, mPlayListButton;

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


        mRecordButton.setOnClickListener(this);
        mPlayListButton.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if (v == mRecordButton) {
            startRecord();
        } else if (v == mPlayListButton) {
            showList(this);
        }
    }

    private void startRecord() {
        @SuppressLint("SimpleDateFormat") String fileName = new SimpleDateFormat("yyyyMMddhhmmss").format(new Date());
        if (mAudioRecorder == null) {
            mAudioRecorder = new AudioRecorder(this);
            mAudioRecorder.createDefaultAudio(fileName);
            mAudioRecorder.startRecord();
            mRecordButton.setText("停止录音");
        } else {
            mAudioRecorder.release();
            mAudioRecorder = null;
            mRecordButton.setText("开始录音");
        }
    }

    private void showList(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, 0);
        builder.setTitle("视频列表");
        new Thread(() -> {
            //读取存储卡跟目录mp4视频
            File dir = context.getExternalFilesDir(null);
            File[] files = dir.listFiles();

            List<Item> items = new ArrayList<>();
            if (files != null) {
                for (File file : files) {
                    items.add(new Item(file.getName(), file.getAbsolutePath()));
                }
            }

            runOnUiThread(() -> {
                if (items == null || items.size() == 0) {
                    Toast.makeText(context, "获取数据为空", Toast.LENGTH_LONG).show();
                    return;
                }
                builder.setAdapter(new FileAdapter(context, items), (dialog, which) -> {
                    dialog.dismiss();
                    Item file = items.get(which);
//                                mPresenter.playWithUri(file.getUri());
                });
                builder.create().show();
            });
        }).start();
    }

    //申请录音权限
    private static final int GET_RECODE_AUDIO = 1;

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
                ActivityCompat.requestPermissions(activity, sPermissions, GET_RECODE_AUDIO);
                break;
            }
        }
    }
}