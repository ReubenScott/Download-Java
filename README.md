# m3u8Dowload
<h1>java下载m3u8视频，解密并合并ts</h1>
<h2>
m3u8链接获取方式以及代码分析请参见：
</h2>
https://blog.csdn.net/qq494257084/article/details/103550171
<h3>准备</h3>
<pre>
JDK：1.8
开发环境：IntelliJ IDEA
用于解密的jar包：bcprov-jdk16-139.jar
了解一些m3u8标签作用
https://www.cnblogs.com/shakin/p/3870442.html
</pre>
<h3>使用方法</h3>
<pre>
M3u8DownloadFactory.M3u8Download m3u8Download =  M3u8DownloadFactory.getInstance(M3U8URL);
//设置生成目录
m3u8Download.setDir("F://m3u8JavaTest");
//设置视频名称
m3u8Download.setFileName("test");
//设置线程数
m3u8Download.setThreadCount(100);
//设置重试次数
m3u8Download.setRetryCount(100);
//设置连接超时时间（单位：毫秒）
m3u8Download.setTimeoutMillisecond(10000L);
//开始下载
m3u8Download.start();
</pre>
