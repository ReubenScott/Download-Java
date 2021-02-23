package com.kindustry.net.m4s;

import crawler.utils.GetWebData;
import crawler.utils.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Author: xiaoshijiu
 * @Date: 2020/8/10
 * @Description: $value$
 */
public class CrawlerBilibiliSerachViedo {

    private static Pattern pattern = Pattern.compile("\"baseUrl\":\".+?\"");

    private static CountDownLatch countDownLatch = new CountDownLatch(12);

    /**
     * 已经下载的bytes大小（使用线程安全的类）
     */
    private static AtomicLong downloadBytes = new AtomicLong();

    /**
     * 上一秒已经下载的bytes大小
     */
    private static long lastDownloadBytes;

    public static void main(String[] args) {
        long begin = System.currentTimeMillis();
        Map<String, String> map = new HashMap<>();
        map.put("user-agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3809.100 Safari/537.36");
        String data = GetWebData.getWebDataWithHeaders(
                "https://search.bilibili.com/all?keyword=%E9%87%91%E6%B3%AB%E9%9B%85", "get",
                "utf-8", map);
        Document document = Jsoup.parse(data);
        Elements elements = document.select(".video-list>li");
        // 存放视频打开链接
        List<String> viedoLinkList = new ArrayList<>();
        for (Element element : elements) {
            viedoLinkList.add("https:" + element.select("a").first().attr("href"));
        }
        int len = viedoLinkList.size();
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(20, 50, 500, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(len));
        // 为避免ip被封，只爬六个视频
        for (int i = 0; i < 6; i++) {
            poolExecutor.execute(downloadViedo(viedoLinkList.get(i), map, i, poolExecutor));
        }
        // 开启下载速度监控
        ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1);
        executorService.scheduleAtFixedRate(() -> {
            long speed = downloadBytes.get() - lastDownloadBytes;
            System.out.println("下载速度：" + StringUtils.convertToDownloadSpeed(speed));
            lastDownloadBytes = downloadBytes.get();
        }, 1, 1, TimeUnit.SECONDS);
        // 利用countdownlatch，实现main线程等待任务线程，达到监控效果
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 必须关闭线程池，不然进程不会结束
        poolExecutor.shutdown();
        // 关闭定时
        executorService.shutdown();
        // 计时
        double time = (System.currentTimeMillis() - begin) / 60000.00;
        System.out.printf("完成，总用时：%.2f分钟\n", time);
    }

    private static Runnable downloadViedo(String url, Map<String, String> map, int i,
            ThreadPoolExecutor poolExecutor) {
        return () -> {
            Map<String, String> map1 = new HashMap<>();
            map1.putAll(map);
            String data = GetWebData.getWebDataWithHeaders(url, "get", "utf-8", map);
            // 正则匹配到m4s链接，加入到集合中（取第一个为视频链接，最后一个为音频链接）
            List<String> list = new ArrayList<>();
            Matcher matcher = pattern.matcher(data);
            while (matcher.find()) {
                list.add(matcher.group(0).split("\"baseUrl\":\"")[1].replace("\"", ""));
            }
            // 试探找出视频和音频的最大长度
            map1.put("Range", "bytes=0-100");
            map1.put("Referer", url);
            map1.put("Origin", "https://www.bilibili.com");
            map1.put("Sec-Fetch-Mode", "cors");
            Map<String, String> map2 = new HashMap<>();
            map2.putAll(map1);
            Map<String, String> vHeaders = GetWebData.getWebDataResponseHeaders(list.get(0), "get",
                    map1);
            String vLength = vHeaders.get("Content-Range");
            int vBytes = Integer.parseInt(vLength.split("/")[1]);
            Map<String, String> aHeaders = GetWebData
                    .getWebDataResponseHeaders(list.get(list.size() - 1), "get", map1);
            String aLength = aHeaders.get("Content-Range");
            int aBytes = Integer.parseInt(aLength.split("/")[1]);
            // 修改请求大小，下载视频和音频
            map1.put("Range", "bytes=0-" + vBytes);
            map2.put("Range", "bytes=0-" + aBytes);
            poolExecutor.execute(() -> {
                downFilesWithHeaders(list.get(0), "K:/crawler/bilibili/search", i + ".mp4", map1);
                countDownLatch.countDown();
            });
            poolExecutor.execute(() -> {
                downFilesWithHeaders(list.get(list.size() - 2), "K:/crawler/bilibili/search",
                        i + ".mp3", map2);
                countDownLatch.countDown();
            });
        };
    }
