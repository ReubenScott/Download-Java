package com.kindustry.crawler.m4s;

/*
 * Copyright (c) 2020, 2021 Daylam Tayari <daylam@tayari.gg>
 * 
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License version 3as published by the Free Software Foundation.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program. If not see http://www.gnu.org/licenses/ or write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * @author Daylam Tayari daylam@tayari.gg https://github.com/daylamtayari
 * 
 * @version 1.0 Github project home page: https://github.com/daylamtayari/M3U8-Downloader
 */

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.NavigableMap;
import java.util.Scanner;

import com.kindustry.framework.service.Worker;
import com.kindustry.net.m3u8.download.M3u8DownloadFactory;
import com.kindustry.net.m3u8.listener.DownloadListener;
import com.kindustry.net.m3u8.utils.Constant;

/**
 * This is the main class which contains the main method of the program.
 */
public class DownloadWorker implements Worker {

  /**
   * Main method of the program.
   * 
   * @param args
   */
  public void job() {

    // String uri = "https://filegroup.gtv.org/group6/vm3u8/20210218/19/19/602ebdda226e775907454f2f/hls.m3u8";
    String url = "https://filegroup.gtv.org/group5/vm3u8/20201223/22/26/5fe3c40093a06b22d9e0cd98/hls.m3u8";
    // String folder = "F:/TEMP/";
    // This assumes that the folder path entered ended with a '\'.
    // When implementing this into an actual program, add a '\' at the end if the user did not.
    // String name = "test_down";
    // String fp = folder + name + ".ts";
    // FileHandler.createTempFolder();

    /*  
     *   HttpsURLConnection httpURLConnection = HttpUtil.getConnection(url, HttpUtil.getSocksProxy("127.0.0.1", 8580));
        InputStream inputStream;
        try {
          inputStream = httpURLConnection.getInputStream();
          OutputStream outputStream = null;
          File file2 = new File(folder + Constant.FILESEPARATOR + name + ".xy");
          byte[] bytes = new byte[4096];
          try {
            outputStream = new FileOutputStream(file2);
          } catch (FileNotFoundException e) {
            e.printStackTrace();
          }
          int len;
          // 将未解密的ts片段写入文件
          while ((len = inputStream.read(bytes)) != -1) {
            outputStream.write(bytes, 0, len);
          }
          outputStream.flush();
          inputStream.close();
          outputStream.close();
        } catch (IOException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }
        */

    M3u8DownloadFactory.M3u8Download m3u8Download = M3u8DownloadFactory.getInstance(url);
    // 设置生成目录
    m3u8Download.setDir("F://m3u8JavaTest1");
    // 设置视频名称
    m3u8Download.setFileName("The Fall of Cabal Sequel EP5");
    // 设置线程数
    m3u8Download.setThreadCount(100);
    // 设置重试次数
    m3u8Download.setRetryCount(100);
    // 设置连接超时时间（单位：毫秒）
    m3u8Download.setTimeoutMillisecond(10000L);
    /*
    设置日志级别
    可选值：NONE INFO DEBUG ERROR
    */
    m3u8Download.setLogLevel(Constant.INFO);
    // 设置监听器间隔（单位：毫秒）
    m3u8Download.setInterval(500L);
    // 添加额外请求头
    /*  Map<String, Object> headersMap = new HashMap<>();
      headersMap.put("Content-Type", "text/html;charset=utf-8");
      m3u8Download.addRequestHeaderMap(headersMap);*/
    // 添加监听器
    m3u8Download.addListener(new DownloadListener() {
      @Override
      public void start() {
        System.out.println("开始下载！");
      }

      @Override
      public void process(String downloadUrl, int finished, int sum, float percent) {
        System.out.println("下载网址：" + downloadUrl + "\t已下载" + finished + "个\t一共" + sum + "个\t已完成" + percent + "%");
      }

      @Override
      public void speed(String speedPerSecond) {
        System.out.println("下载速度：" + speedPerSecond);
      }

      @Override
      public void end() {
        System.out.println("下载完毕");
      }
    });

    // 开始下载
    m3u8Download.start();

  }

  /**
   * Main method of the program.
   * 
   * @param args
   */
  public static void main1(String[] args) {
    System.out.print("\nM3U8 Downloader:" + "\nThis is a proof of concept program for a M3U8 downloader using Java."
      + "\nAuthor: Daylam Tayari https://github.com/daylamtayari https://paypal.me/daylamtayari" + "\n");
    Scanner sc = new Scanner(System.in);
    System.out.print("\nM3U8 URL: ");
    String url = sc.next();
    System.out.print("\nOutput FOLDER path: ");
    String folder = sc.next();
    // This assumes that the folder path entered ended with a '\'.
    // When implementing this into an actual program, add a '\' at the end if the user did not.
    System.out.print("\nOutput file name: ");
    String name = sc.next();
    String fp = folder + name + ".ts";
    sc.close();
    FileHandler.createTempFolder();
    ArrayList<String> chunks = null;
    try {
      chunks = Downloader.getChunks(url);
    } catch (IOException e) {
    }
    NavigableMap<Integer, File> segmentMap = Downloader.TSDownload(chunks);
    FileHandler.mergeFile(segmentMap, fp);
    System.out.print("\nDownload complete!" + "\nFile downloaded at: " + fp);
  }

}