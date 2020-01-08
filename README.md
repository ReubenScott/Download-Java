# m3u8Dowload
<h1>java下载m3u8视频，解密并合并ts</h1>
<h2>
m3u8链接获取方式以及代码分析请参见：
</h2>
https://blog.csdn.net/qq494257084/article/details/103551293
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

<h4>2020.01.08</h4>
* 优化内存占用率

<h4>2019.12.24</h4>
* 进制转换问题

<h4>2019.12.23</h4>
* 文件路径优化 
* 解决新合并的文件会把以前的删除的问题

<h4>2019.12.21</h4>
* 修复key获取错误
* 更新使用说明
* 解决内存占用过多 
* 解决链接获取错误 
* 增加IV向量偏移解密
* 解决合并文件后程序并没有退出