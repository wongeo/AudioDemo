package com.feng.audiodemo.audio;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class FileSource implements ISource {

    private InputStream is;

    public FileSource(String filePath) throws FileNotFoundException {
        is = new FileInputStream(filePath);
    }

    @Override
    public int read(byte[] data) throws IOException {
        return is.read(data);
    }

    @Override
    public void close() throws IOException {
        is.close();
    }
}
