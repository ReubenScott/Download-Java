package com.kindustry.crawler.info;

import java.net.InetSocketAddress;
import java.net.Proxy;

import com.kindustry.config.constant.ApplicationConstant;
import com.kindustry.framework.generator.UUIDWorker;

public class MediaInfo {

  public enum States {
    QUEUE, EXTRACTING, EXTRACTING_DONE, DOWNLOADING, RETRYING, DONE, ERROR, STOP
  }

  // user friendly url (not direct video stream url)
  private String url;

  private String title;

  private Proxy proxy;

  // states, three variables
  private States state;

  public MediaInfo(String url) {
    this.url = url;
    this.title = UUIDWorker.generate();
    this.state = States.QUEUE;
  }

  public MediaInfo(String url, Proxy.Type proxyType, String addr, int port) {
    this.url = url;
    this.title = UUIDWorker.generate();
    this.proxy = new Proxy(proxyType, new InetSocketAddress(addr, port));
    this.state = States.QUEUE;
  }

  public String getPath() {
    return ApplicationConstant.TEMP_DIR + title;
  }

  /**
   * urlを取得する。
   * 
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * urlを設定する。
   * 
   * @param url
   *          the url to set
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * titleを取得する。
   * 
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * titleを設定する。
   * 
   * @param title
   *          the title to set
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * proxyを取得する。
   * 
   * @return the proxy
   */
  public Proxy getProxy() {
    return proxy;
  }

  /**
   * proxyを設定する。
   * 
   * @param proxy
   *          the proxy to set
   */
  public void setProxy(Proxy proxy) {
    this.proxy = proxy;
  }

  /**
   * stateを取得する。
   * 
   * @return the state
   */
  public States getState() {
    return state;
  }

  /**
   * stateを設定する。
   * 
   * @param state
   *          the state to set
   */
  public void setState(States state) {
    this.state = state;
  }

  // private List<VideoFileInfo> info = new ArrayList<VideoFileInfo>();

}