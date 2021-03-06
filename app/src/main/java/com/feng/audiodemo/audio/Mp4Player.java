package com.feng.audiodemo.audio;


import android.content.Context;
import android.view.Surface;

public class Mp4Player implements IPlayer {
    private AudioPlayer mAudioPlayer;

    public Mp4Player(Context context) {
        mAudioPlayer = new AudioPlayer(context);
    }

    public void setSurface(Surface surface) {
        mAudioPlayer.setSurface(surface);
    }

    @Override
    public void setDataSource(String uri) {
        mAudioPlayer.setDataSource(uri);
    }

    @Override
    public void prepare() {
        mAudioPlayer.prepare();
    }

    @Override
    public void start() {
        mAudioPlayer.start();
    }

    @Override
    public void stop() {
        mAudioPlayer.stop();
    }

    @Override
    public void setOnStateChangeListener(OnStateChangeListener listener) {
        mAudioPlayer.setOnStateChangeListener(listener);
    }

    @Override
    public void setOnErrorListener(OnErrorListener listener) {
        mAudioPlayer.setOnErrorListener(listener);
    }

    @Override
    public State getState() {
        return mAudioPlayer.getState();
    }

    @Override
    public void pause() {
        mAudioPlayer.pause();
    }
}
