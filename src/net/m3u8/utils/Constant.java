package net.m3u8.utils;

/**
 * 常量
 *
 * @author liyaling
 * @email ts_liyaling@qq.com
 * @date 2019/12/23 10:11
 */

public class Constant {

    //文件分隔符，在window中为\\，在linux中为/
    public static final String FILESEPARATOR = System.getProperty("file.separator");

    //因子
    public static final float FACTOR = 1.15F;

    //默认文件每次读取字节数
    public static final int BYTE_COUNT = 40960;

}
