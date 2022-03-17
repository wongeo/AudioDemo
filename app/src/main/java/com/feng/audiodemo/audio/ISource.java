package com.feng.audiodemo.audio;

import android.media.MediaFormat;

import java.io.Closeable;
import java.io.IOException;

/**
 * 数据源
 */
public interface ISource extends Closeable {

    MediaFormat getMediaFormat();

    int read(byte data[]) throws IOException;

    static void close(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
