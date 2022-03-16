package com.feng.audiodemo.audio;

public interface IPlayer {

    void setDataSource(String uri);

    void prepare();

    void start();

    void stop();

    void setOnStateChangeListener(OnStateChangeListener listener);

    void setOnErrorListener(OnErrorListener listener);

    State getState();

    void pause();

    enum State {
        NONE,
        LOADING,
        START,
        PAUSE,
        ERROR,
        STOP,
    }

    interface OnStateChangeListener {

        void onChange(State src, State desc);


    }

    interface OnErrorListener {
        void onError(int code, String msg);
    }
}
