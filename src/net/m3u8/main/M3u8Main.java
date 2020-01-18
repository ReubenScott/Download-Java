package net.m3u8.main;

import net.m3u8.download.M3u8DownloadFactory;

/**
 * @author liyaling
 * @email ts_liyaling@qq.com
 * @date 2019/12/14 16:02
 */

public class M3u8Main {

    private static final String M3U8URL = "https://videozmcdn.stz8.com:8091/20191127/PK7a0LKQ/index.m3u8";

    public static void main(String[] args) {

        M3u8DownloadFactory.M3u8Download m3u8Download =  M3u8DownloadFactory.getInstance(M3U8URL);
        //设置生成目录
        m3u8Download.setDir("F://m3u8JavaTest1");
        //设置视频名称
        m3u8Download.setFileName("test");
        //设置线程数
        m3u8Download.setThreadCount(100);
        //设置重试次数
        m3u8Download.setRetryCount(100);
        //设置连接超时时间（单位：毫秒）
        m3u8Download.setTimeoutMillisecond(10000L);
        m3u8Download.start();
    }
}
