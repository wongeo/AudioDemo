package com.feng.audiodemo.audio;

public final class WavHeader {
    /**
     * RIFF数据块
     */
    final String riffChunkId = "RIFF";
    int riffChunkSize;
    final String riffType = "WAVE";

    /**
     * FORMAT 数据块
     */
    final String formatChunkId = "fmt ";
    final int formatChunkSize = 16;
    final short audioFormat = 1;
    short channels;
    int sampleRate;
    int byteRate;
    short blockAlign;
    short sampleBits;

    /**
     * FORMAT 数据块
     */
    final String dataChunkId = "data";
    int dataChunkSize;

    private WavHeader(int totalAudioLen, int sampleRate, short channels, short sampleBits) {
        this.riffChunkSize = totalAudioLen;
        this.channels = channels;
        this.sampleRate = sampleRate;
        this.byteRate = sampleRate * sampleBits / 8 * channels;
        this.blockAlign = (short) (channels * sampleBits / 8);
        this.sampleBits = sampleBits;
        this.dataChunkSize = totalAudioLen - 44;
    }

    /**
     * https://www.jianshu.com/p/90c77197f1d4
     * <p>
     * 生成wav格式的Header
     * <p>
     * wave是RIFF文件结构，每一部分为一个chunk，其中有RIFF WAVE chunk，
     * FMT Chunk，Fact chunk（可选）,Data chunk
     *
     * @param totalAudioLen 不包括header的音频数据总长度
     * @param sampleRate    采样率,也就是录制时使用的频率
     * @param channels      audioRecord的频道数量
     * @param sampleBits    位宽
     */
    public static WavHeader create(int totalAudioLen, int sampleRate, short channels, short sampleBits) {
        return new WavHeader(totalAudioLen, sampleRate, channels, sampleBits);
    }

    /**
     * 转换为字节数组
     */
    public byte[] toBytes() {
        byte[] result;
        result = merger(toBytes(riffChunkId), toBytes(riffChunkSize));
        result = merger(result, toBytes(riffType));
        result = merger(result, toBytes(formatChunkId));
        result = merger(result, toBytes(formatChunkSize));
        result = merger(result, toBytes(audioFormat));
        result = merger(result, toBytes(channels));
        result = merger(result, toBytes(sampleRate));
        result = merger(result, toBytes(byteRate));
        result = merger(result, toBytes(blockAlign));
        result = merger(result, toBytes(sampleBits));
        result = merger(result, toBytes(dataChunkId));
        result = merger(result, toBytes(dataChunkSize));
        return result;
    }

    public static byte[] merger(byte[] bt1, byte[] bt2) {
        byte[] bt3 = new byte[bt1.length + bt2.length];
        System.arraycopy(bt1, 0, bt3, 0, bt1.length);
        System.arraycopy(bt2, 0, bt3, bt1.length, bt2.length);
        return bt3;
    }

    /**
     * int 转 byte[]
     */
    public static byte[] toBytes(int i) {
        byte[] b = new byte[4];
        b[0] = (byte) (i & 0xff);
        b[1] = (byte) ((i >> 8) & 0xff);
        b[2] = (byte) ((i >> 16) & 0xff);
        b[3] = (byte) ((i >> 24) & 0xff);
        return b;
    }

    /**
     * short 转 byte[]
     */
    public static byte[] toBytes(short src) {
        byte[] dest = new byte[2];
        dest[0] = (byte) (src);
        dest[1] = (byte) (src >> 8);
        return dest;
    }


    /**
     * String 转 byte[]
     */
    public static byte[] toBytes(String str) {
        return str.getBytes();
    }
}