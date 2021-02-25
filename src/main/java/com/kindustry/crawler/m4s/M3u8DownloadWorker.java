package com.kindustry.crawler.m4s;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.HttpsURLConnection;

import com.kindustry.common.http.HttpUtil;
import com.kindustry.framework.service.Worker;
import com.kindustry.net.m3u8.Exception.M3u8Exception;
import com.kindustry.net.m3u8.listener.DownloadListener;
import com.kindustry.net.m3u8.utils.Constant;
import com.kindustry.net.m3u8.utils.Log;
import com.kindustry.net.m3u8.utils.MediaFormat;
import com.kindustry.net.m3u8.utils.StringUtils;

public class M3u8DownloadWorker implements Worker {

  // 要下载的m3u8链接
  private DownloadInfo downloadInfo;

  // 优化内存占用
  // private static final BlockingQueue<byte[]> BLOCKING_QUEUE = new LinkedBlockingQueue<>();

  // 线程数
  private int threadCount = 1;

  // 重试次数
  private int retryCount = 30;

  // 合并后的文件存储目录
  private String dir;

  // 合并后的视频文件名称
  private String fileName;

  // 已完成ts片段个数
  private int finishedCount = 0;

  // 解密算法名称
  private String method;

  // 密钥
  private String key = "";

  // 密钥字节
  private byte[] keyBytes = new byte[16];

  // key是否为字节
  private boolean isByte = false;

  // IV
  private String iv = "";

  // 所有ts片段下载链接
  private Set<String> tsSet = new LinkedHashSet<>();

  // 解密后的片段
  private Set<File> finishedFiles = new ConcurrentSkipListSet<>(Comparator.comparingInt(o -> Integer.parseInt(o.getName().replace(".xyz", ""))));

  // 已经下载的文件大小
  private BigDecimal downloadBytes = new BigDecimal(0);

  // 监听间隔
  private volatile long interval = 0L;

  // 监听事件
  private Set<DownloadListener> listenerSet = new HashSet<>(5);

  /**
   * 
   * @param DOWNLOADURL
   */
  public M3u8DownloadWorker(DownloadInfo downloadInfo) {
    this.downloadInfo = downloadInfo;
  }

  /**
   * 1. 开始下载视频
   */
  @Override
  public void job() {
    setThreadCount(30);
    checkField();

    // 获取所有的ts片段下载链接
    String tsUrl = getTsUrl();

    if (StringUtils.isEmpty(tsUrl)) {
      Log.i("不需要解密");
    }

    // 线程池
    // final ExecutorService fixedThreadPool = SchedulerThreadManager.getInstance().getThreadPoolExecutor();
    final ExecutorService fixedThreadPool = Executors.newFixedThreadPool(threadCount);

    int i = 0;

    // 如果生成目录不存在，则创建
    File tempdir = new File(Constant.TEMPDIR + downloadInfo.getTitle());
    if (!tempdir.exists()) {
      tempdir.mkdirs();
    }

    // 执行多线程下载
    for (String s : tsSet) {
      i++;
      fixedThreadPool.execute(multiDownload(s, i));
    }
    fixedThreadPool.shutdown();

    // 下载过程监视
    new Thread(() -> {
      int consume = 0;
      // 轮询是否下载成功
      while (!fixedThreadPool.isTerminated()) {
        try {
          consume++;
          BigDecimal bigDecimal = new BigDecimal(downloadBytes.toString());
          Thread.sleep(1000L);
          Log.i("已用时" + consume + "秒！\t下载速度：" + StringUtils.convertToDownloadSpeed(new BigDecimal(downloadBytes.toString()).subtract(bigDecimal), 3) + "/s");
          Log.i("\t已完成" + finishedCount + "个，还剩" + (tsSet.size() - finishedCount) + "个！");
          Log.i(new BigDecimal(finishedCount).divide(new BigDecimal(tsSet.size()), 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100)).setScale(2, BigDecimal.ROUND_HALF_UP)
            + "%");
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      Log.i("下载完成，正在合并文件！共" + finishedFiles.size() + "个！" + StringUtils.convertToDownloadSpeed(downloadBytes, 3));
      // 开始合并视频
      mergeTs();
      // 删除多余的ts片段
      deleteFiles();
      Log.i("视频合并完成，欢迎使用!");
    }).start();
    // startListener(fixedThreadPool);
  }

  /**
   * 2. 获取所有的ts片段下载链接
   *
   * @return 链接是否被加密，null为非加密
   */
  private String getTsUrl() {
    StringBuilder content = getUrlContent(downloadInfo.getUrl(), false);
    // 判断是否是m3u8链接
    if (!content.toString().contains("#EXTM3U")) throw new M3u8Exception(downloadInfo.getUrl() + "不是m3u8链接！");
    String[] split = content.toString().split("\\n");
    String keyUrl = "";
    boolean isKey = false;
    for (String s : split) {
      // 如果含有此字段，则说明只有一层m3u8链接
      if (s.contains("#EXT-X-KEY") || s.contains("#EXTINF")) {
        isKey = true;
        keyUrl = downloadInfo.getUrl();
        break;
      }
      // 如果含有此字段，则说明ts片段链接需要从第二个m3u8链接获取
      if (s.contains(".m3u8")) {
        if (StringUtils.isUrl(s)) return s;
        String relativeUrl = downloadInfo.getUrl().substring(0, downloadInfo.getUrl().lastIndexOf("/") + 1);
        if (s.startsWith("/")) s = s.replaceFirst("/", "");
        keyUrl = mergeUrl(relativeUrl, s);
        break;
      }
    }
    if (StringUtils.isEmpty(keyUrl)) throw new M3u8Exception("未发现key链接！");
    // 获取密钥
    String key1 = isKey ? getKey(keyUrl, content) : getKey(keyUrl, null);
    if (StringUtils.isNotEmpty(key1))
      key = key1;
    else
      key = null;
    return key;
  }

  /**
   * 4.获取ts解密的密钥，并把ts片段加入set集合
   *
   * @param url
   *          密钥链接，如果无密钥的m3u8，则此字段可为空
   * @param content
   *          内容，如果有密钥，则此字段可以为空
   * @return ts是否需要解密，null为不解密
   */
  private String getKey(String url, StringBuilder content) {
    StringBuilder urlContent;
    if (content == null || StringUtils.isEmpty(content.toString()))
      urlContent = getUrlContent(url, false);
    else
      urlContent = content;
    if (!urlContent.toString().contains("#EXTM3U")) throw new M3u8Exception(downloadInfo.getUrl() + "不是m3u8链接！");
    String[] split = urlContent.toString().split("\\n");
    for (String s : split) {
      // 如果含有此字段，则获取加密算法以及获取密钥的链接
      if (s.contains("EXT-X-KEY")) {
        String[] split1 = s.split(",");
        for (String s1 : split1) {
          if (s1.contains("METHOD")) {
            method = s1.split("=", 2)[1];
            continue;
          }
          if (s1.contains("URI")) {
            key = s1.split("=", 2)[1];
            continue;
          }
          if (s1.contains("IV")) iv = s1.split("=", 2)[1];
        }
      }
    }
    String relativeUrl = url.substring(0, url.lastIndexOf("/") + 1);
    // 将ts片段链接加入set集合
    for (int i = 0; i < split.length; i++) {
      String s = split[i];
      if (s.contains("#EXTINF")) {
        String s1 = split[++i];
        tsSet.add(StringUtils.isUrl(s1) ? s1 : mergeUrl(relativeUrl, s1));
      }
    }
    if (!StringUtils.isEmpty(key)) {
      key = key.replace("\"", "");
      return getUrlContent(StringUtils.isUrl(key) ? key : mergeUrl(relativeUrl, key), true).toString().replaceAll("\\s+", "");
    }
    return null;
  }

  /**
   * 3 .模拟http请求获取内容
   *
   * @param urls
   *          http链接
   * @param isKey
   *          这个url链接是否用于获取key
   * @return 内容
   */
  private StringBuilder getUrlContent(String urls, boolean isKey) {
    int count = 1;
    HttpURLConnection httpURLConnection = null;
    StringBuilder content = new StringBuilder();
    while (count <= retryCount) {
      try {
        URL url = new URL(urls);
        httpURLConnection = HttpUtil.getConnection(urls, downloadInfo.getProxy());
        String line;
        InputStream inputStream = httpURLConnection.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        if (isKey) {
          byte[] bytes = new byte[128];
          int len;
          len = inputStream.read(bytes);
          isByte = true;
          if (len == 1 << 4) {
            keyBytes = Arrays.copyOf(bytes, 16);
            content.append("isByte");
          } else
            content.append(new String(Arrays.copyOf(bytes, len)));
          return content;
        }
        while ((line = bufferedReader.readLine()) != null)
          content.append(line).append("\n");
        bufferedReader.close();
        inputStream.close();
        Log.i(content);
        break;
      } catch (Exception e) {
        Log.d("第" + count + "获取链接重试！\t" + urls);
        count++;
        // e.printStackTrace();
      } finally {
        if (httpURLConnection != null) {
          httpURLConnection.disconnect();
        }
      }
    }
    if (count > retryCount) throw new M3u8Exception("连接超时！");
    return content;
  }

  /**
   * 开启下载线程
   *
   * @param urls
   *          ts片段链接
   * @param i
   *          ts片段序号
   * @return 线程
   */
  private Thread multiDownload(String urls, int i) {
    return new Thread(() -> {
      int count = 1;
      HttpsURLConnection httpURLConnection = null;
      // xy为未解密的ts片段，如果存在，则删除
      File file2 = new File(downloadInfo.getPath() + Constant.FILESEPARATOR + i + ".xy");
      if (file2.exists()) file2.delete();
      OutputStream outputStream = null;
      InputStream inputStream1 = null;
      FileOutputStream outputStream1 = null;
      byte[] bytes = new byte[4096];
      // try {
      // bytes = BLOCKING_QUEUE.take();
      // } catch (InterruptedException e) {
      // bytes = new byte[Constant.BYTE_COUNT];
      // }
      // 重试次数判断
      while (count <= retryCount) {
        try {
          // 模拟http请求获取ts片段文件
          httpURLConnection = HttpUtil.getConnection(urls, downloadInfo.getProxy());
          InputStream inputStream = httpURLConnection.getInputStream();
          try {
            outputStream = new FileOutputStream(file2);
          } catch (FileNotFoundException e) {
            e.printStackTrace();
            continue;
          }
          int len;
          // 将未解密的ts片段写入文件
          while ((len = inputStream.read(bytes)) != -1) {
            outputStream.write(bytes, 0, len);
            synchronized (this) {
              downloadBytes = downloadBytes.add(new BigDecimal(len));
            }
          }
          outputStream.flush();
          inputStream.close();
          inputStream1 = new FileInputStream(file2);
          int available = inputStream1.available();
          if (bytes.length < available) bytes = new byte[available];
          inputStream1.read(bytes);
          File file = new File(downloadInfo.getPath() + Constant.FILESEPARATOR + i + ".xyz");
          outputStream1 = new FileOutputStream(file);
          // 开始解密ts片段，这里我们把ts后缀改为了xyz，改不改都一样
          byte[] decrypt = decrypt(bytes, available, key, iv, method);
          if (decrypt == null)
            outputStream1.write(bytes, 0, available);
          else
            outputStream1.write(decrypt);
          finishedFiles.add(file);
          break;
        } catch (Exception e) {
          if (e instanceof InvalidKeyException || e instanceof InvalidAlgorithmParameterException) {
            Log.e("解密失败！");
            break;
          }
          Log.d("第" + count + "获取链接重试！\t" + urls);
          count++;
          // e.printStackTrace();
        } finally {
          try {
            if (inputStream1 != null) inputStream1.close();
            if (outputStream1 != null) outputStream1.close();
            if (outputStream != null) outputStream.close();
            // BLOCKING_QUEUE.put(bytes);
          } catch (IOException e) {
            e.printStackTrace();
          }
          if (httpURLConnection != null) {
            httpURLConnection.disconnect();
          }
        }
      }
      if (count > retryCount)
      // 自定义异常
        throw new M3u8Exception("连接超时！");
      finishedCount++;
      // Log.i(urls + "下载完毕！\t已完成" + finishedCount + "个，还剩" + (tsSet.size() - finishedCount) + "个！");
    });
  }

  private void startListener(ExecutorService fixedThreadPool) {
    new Thread(() -> {
      for (DownloadListener downloadListener : listenerSet)
        downloadListener.start();
      // 轮询是否下载成功
      while (!fixedThreadPool.isTerminated()) {
        try {
          Thread.sleep(interval);
          for (DownloadListener downloadListener : listenerSet)
            downloadListener.process(downloadInfo.getUrl(), finishedCount, tsSet.size(),
              new BigDecimal(finishedCount).divide(new BigDecimal(tsSet.size()), 4, BigDecimal.ROUND_HALF_UP).multiply(new BigDecimal(100)).setScale(2, BigDecimal.ROUND_HALF_UP)
                .floatValue());
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
      for (DownloadListener downloadListener : listenerSet)
        downloadListener.end();
    }).start();
    new Thread(() -> {
      while (!fixedThreadPool.isTerminated()) {
        try {
          BigDecimal bigDecimal = new BigDecimal(downloadBytes.toString());
          Thread.sleep(1000L);
          for (DownloadListener downloadListener : listenerSet)
            downloadListener.speed(StringUtils.convertToDownloadSpeed(new BigDecimal(downloadBytes.toString()).subtract(bigDecimal), 3) + "/s");
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }).start();
  }

  /**
   * 合并下载好的ts片段
   */
  private void mergeTs() {
    try {
      File file = new File(dir + Constant.FILESEPARATOR + fileName + ".mp4");
      System.gc();
      if (file.exists())
        file.delete();
      else
        file.createNewFile();
      FileOutputStream fileOutputStream = new FileOutputStream(file);
      byte[] b = new byte[4096];
      for (File f : finishedFiles) {
        FileInputStream fileInputStream = new FileInputStream(f);
        int len;
        while ((len = fileInputStream.read(b)) != -1) {
          fileOutputStream.write(b, 0, len);
        }
        fileInputStream.close();
        fileOutputStream.flush();
      }
      fileOutputStream.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * 删除下载好的片段
   */
  private void deleteFiles() {
    File file = new File(downloadInfo.getPath());
    for (File f : file.listFiles()) {
      if (f.getName().endsWith(".xy") || f.getName().endsWith(".xyz")) f.delete();
    }
  }

  /**
   * 解密ts
   *
   * @param sSrc
   *          ts文件字节数组
   * @param length
   * @param sKey
   *          密钥
   * @return 解密后的字节数组
   */
  private byte[] decrypt(byte[] sSrc, int length, String sKey, String iv, String method) throws Exception {
    if (StringUtils.isNotEmpty(method) && !method.contains("AES")) throw new M3u8Exception("未知的算法！");
    // 判断Key是否正确
    if (StringUtils.isEmpty(sKey)) return null;
    // 判断Key是否为16位
    if (sKey.length() != 16 && !isByte) {
      throw new M3u8Exception("Key长度不是16位！");
    }
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
    SecretKeySpec keySpec = new SecretKeySpec(isByte ? keyBytes : sKey.getBytes(StandardCharsets.UTF_8), "AES");
    byte[] ivByte;
    if (iv.startsWith("0x"))
      ivByte = StringUtils.hexStringToByteArray(iv.substring(2));
    else
      ivByte = iv.getBytes();
    if (ivByte.length != 16) ivByte = new byte[16];
    // 如果m3u8有IV标签，那么IvParameterSpec构造函数就把IV标签后的内容转成字节数组传进去
    AlgorithmParameterSpec paramSpec = new IvParameterSpec(ivByte);
    cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec);
    return cipher.doFinal(sSrc, 0, length);
  }

  /**
   * 字段校验
   */
  private void checkField() {
    if ("m3u8".compareTo(MediaFormat.getMediaFormat(downloadInfo.getUrl())) != 0) throw new M3u8Exception(downloadInfo.getUrl() + "不是一个完整m3u8链接！");
    if (threadCount <= 0) throw new M3u8Exception("同时下载线程数只能大于0！");
    if (retryCount < 0) throw new M3u8Exception("重试次数不能小于0！");
    if (StringUtils.isEmpty(fileName)) throw new M3u8Exception("视频名称不能为空！");
    finishedCount = 0;
    method = "";
    key = "";
    isByte = false;
    iv = "";
    tsSet.clear();
    finishedFiles.clear();
    downloadBytes = new BigDecimal(0);
  }

  private String mergeUrl(String start, String end) {
    if (end.startsWith("/")) end = end.replaceFirst("/", "");
    int position = 0;
    String subEnd, tempEnd = end;
    while ((position = end.indexOf("/", position)) != -1) {
      subEnd = end.substring(0, position + 1);
      if (start.endsWith(subEnd)) {
        tempEnd = end.replaceFirst(subEnd, "");
        break;
      }
      ++position;
    }
    return start + tempEnd;
  }

  public int getThreadCount() {
    return threadCount;
  }

  public void setThreadCount(int threadCount) {
    // if (BLOCKING_QUEUE.size() < threadCount) {
    // for (int i = BLOCKING_QUEUE.size(); i < threadCount * Constant.FACTOR; i++) {
    // try {
    // BLOCKING_QUEUE.put(new byte[Constant.BYTE_COUNT]);
    // } catch (InterruptedException ignored) {
    // }
    // }
    // }
    this.threadCount = threadCount;
  }

  public int getRetryCount() {
    return retryCount;
  }

  public void setRetryCount(int retryCount) {
    this.retryCount = retryCount;
  }

  public String getDir() {
    return dir;
  }

  public void setDir(String dir) {
    this.dir = dir;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public int getFinishedCount() {
    return finishedCount;
  }

  public void setLogLevel(int level) {
    Log.setLevel(level);
  }

  public void setInterval(long interval) {
    this.interval = interval;
  }

  public void addListener(DownloadListener downloadListener) {
    listenerSet.add(downloadListener);
  }

}