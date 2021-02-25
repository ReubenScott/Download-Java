package com.github.axet.vget;

import java.net.Proxy;

import org.junit.Test;

import com.kindustry.crawler.m4s.DownloadInfo;
import com.kindustry.crawler.m4s.M3u8DownloadWorker;
import com.kindustry.framework.service.JobService;

public class DownloadServiceTest {

  @Test
  public void testAddJob() {

    String url = "https://filegroup.gtv.org/group6/vm3u8/20210219/13/45/602fc0ec1958515902356f4d/hls.m3u8";

    JobService service = new JobService();
    DownloadInfo downloadInfo = new DownloadInfo(url, Proxy.Type.SOCKS, "127.0.0.1", 8580);
    M3u8DownloadWorker worker = new M3u8DownloadWorker(downloadInfo);

    // 设置生成目录
    worker.setDir("F://m3u8JavaTest1");
    // 设置视频名称
    worker.setFileName("The Fall of Cabal Sequel EP1");
    // 设置线程数
    worker.setThreadCount(100);
    // 设置重试次数
    worker.setRetryCount(100);

    service.addWorker(worker);
  }

}
