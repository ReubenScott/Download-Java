# m3u8Dowload
<h1>java download m3u8 video,decrypt & merge ts slice</h1>
<h2>
How to get a m3u8 link and code analysis please reference:
</h2>
https://blog.csdn.net/qq494257084/article/details/103551293
<h3>preparation</h3>
<pre>
JDK:1.8
IDE：IntelliJ IDEA
dependence：bcprov-jdk16-139.jar
Learn about the functions of the m3u8 tag
https://www.cnblogs.com/shakin/p/3870442.html
</pre>
<h3>example</h3>
<pre>
M3u8DownloadFactory.M3u8Download m3u8Download =  M3u8DownloadFactory.getInstance(M3U8URL);
//set the video directory
m3u8Download.setDir("F://m3u8JavaTest");
//set the video file name
m3u8Download.setFileName("test");
//set thread count 
m3u8Download.setThreadCount(100);
//set retry count
m3u8Download.setRetryCount(100);
//set connection timeout (ms)
m3u8Download.setTimeoutMillisecond(10000L);
//start download
m3u8Download.start();
</pre>

<h4>2020.01.08</h4>
* memory optimization

<h4>2019.12.24</h4>
* solve base conversion problem

<h4>2019.12.23</h4>
* file path optimization 
* fixed the problem that the newly merged files would delete the previous video files

<h4>2019.12.21</h4>
* fix get an incorrect key
* update instructions
* memory optimization
* resolve link retrieval errors
* add IV vector offset decryption
* Resolved the problem that the program didn't exit after merging files