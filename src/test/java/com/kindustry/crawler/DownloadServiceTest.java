package com.kindustry.crawler;

import java.net.Proxy;

import org.junit.Test;

import com.kindustry.crawler.handler.M3u8Downloader;
import com.kindustry.crawler.info.MediaInfo;
import com.kindustry.framework.service.JobService;

public class DownloadServiceTest {

  @Test
  public void testAddJob() {

    String url;
    MediaInfo downloadInfo;
    JobService service = new JobService();

    url = "https://filegroup.gtv.org/group6/vm3u8/20210218/19/19/602ebdda226e775907454f2f/hls.m3u8";
    downloadInfo = new MediaInfo(url, Proxy.Type.SOCKS, "127.0.0.1", 8580);

    // url = "https://tudou.diediao-kuyun.com/20200117/10171_542ddaf9/index.m3u8";
    // url =
    // "https://valipl.cp31.ott.cibntv.net/6572FF28C1332714F48D55D1A/03000600006039069B8BB780000000D4E12FEB-5177-4CE5-BFC8-537090199421-00001.ts?ccode=050F&duration=122&expire=18000&psid=78e8cf5428c940a26d31d5e052489c51445d8&ups_client_netip=&ups_ts=1614357944&ups_userid=&t=92638370135d6df&apscid=&mnid=&utid=0AWfF2oxLjgCAXAVWGw4hXm8&vid=XNTExMTM1NzQ2MA&s=ceec1066223e418b94f7&sp=&bc=2&si=5&eo=1&vkey=B2b390f05ecac9a38ba6e38c67d312fde";
    // downloadInfo = new MediaInfo(url);

    M3u8Downloader worker = new M3u8Downloader(downloadInfo);
    // 设置视频名称
    worker.setFileName(downloadInfo.getTitle());
    // 设置线程数
    worker.setThreadCount(100);
    // 设置重试次数
    worker.setRetryCount(100);

    service.addHandler(worker);
  }
}
