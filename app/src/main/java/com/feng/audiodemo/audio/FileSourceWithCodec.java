package com.feng.audiodemo.audio;

import java.io.IOException;

/**
 * 带有解码功能的数据源
 */
public class FileSourceWithCodec implements ISource {

    private final AudioCodec codec;

    public FileSourceWithCodec(String filePath) throws IOException {
        codec = AudioCodec.create(filePath);
    }

    //上次遗留的字节，需要下次优先追加上
    private byte[] bytes;

    @Override
    public int read(byte[] data) throws IOException {
        int len = 0;//拷贝字节的长度
        int offset = 0;//拷贝字节偏移量，因为是多次拷贝，所以需要记录偏移量
        //把上次遗留的先put进去
        if (bytes != null) {
            System.arraycopy(bytes, 0, data, 0, bytes.length);
            len += bytes.length;
            offset += bytes.length;
        }

        while (true) {
            //读取解码后的数据
            AudioCodec.Data raw = codec.read();
            int code = raw.code;
            if (code == -2 || code == -3) {
                //忽略继续读下一次的
                continue;
            }
            if (code == -1) {
                //数据已经读完
                len = -1;
                break;
            }
            bytes = raw.bytes;
            if (offset + bytes.length > data.length) {
                break;
            }
            System.arraycopy(bytes, 0, data, offset, bytes.length);
            len += bytes.length;
            offset += bytes.length;
        }

        return len;
    }

    @Override
    public void close() throws IOException {

    }

}
