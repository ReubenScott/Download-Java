package com.kindustry.crawler.controller;

import java.net.Proxy;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;

import com.kindustry.crawler.handler.M3u8Downloader;
import com.kindustry.crawler.info.MediaInfo;
import com.kindustry.framework.service.JobService;
import com.kindustry.support.web.BasicWebSupport;

@Controller
@RequestMapping("/download/*")
// 父request请求url
public class DownloadController {
  final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Resource
  protected HttpServletRequest request;

  @Resource
  protected HttpServletResponse response;

  // 获取applicationContext.xml中bean的id为loginService的，并注入
  // @Resource(name = "loginService")
  // private LoginService loginService; // 等价于spring传统注入方式写get和set方法，这样的好处是简洁工整，省去了不必要得代码

  // 请求url地址映射，类似Struts的action-mapping
  @RequestMapping("m3u8")
  public String testLogin(@RequestParam(value = "url") String url) {
    // @RequestParam是指请求url地址映射中必须含有的参数(除非属性required=false)
    // @RequestParam可简写为：@RequestParam("username")

    System.out.println(url);

    MediaInfo downloadInfo;
    JobService service = new JobService();

    // url = "https://filegroup.gtv.org/group6/vm3u8/20210219/13/45/602fc0ec1958515902356f4d/hls.m3u8";
    downloadInfo = new MediaInfo(url, Proxy.Type.SOCKS, "127.0.0.1", 8580);
    // downloadInfo = new MediaInfo(url);

    M3u8Downloader worker = new M3u8Downloader(downloadInfo);

    // 设置视频名称
    worker.setFileName(downloadInfo.getTitle());
    // 设置线程数
    worker.setThreadCount(100);
    // 设置重试次数
    worker.setRetryCount(100);

    service.addHandler(worker);

    boolean flag = true;
    while (flag) {
      switch (downloadInfo.getState()) {
        case DONE:
          flag = false;
          logger.info(downloadInfo.getTitle() + " download start ... ");
          BasicWebSupport.downLoadAcesssory(response, downloadInfo.getTitle() + ".mp4");
          break;
        case STOP:
          logger.info(downloadInfo.getTitle() + " download Failed! ... ");
          flag = false;
          break;
        default:
          break;
      }

    }

    return "loginSuccess";
  }

  @RequestMapping("/download.htm")
  public ModelAndView testLogin3(@RequestParam(value = "filename") String filename) {
    // 同样支持参数为表单对象，类似于Struts的ActionForm，User不需要任何配置，直接写即可

    BasicWebSupport.downLoadAcesssory(response, filename);

    return new ModelAndView("loginSuccess");
  }

  // @RequestMapping("/login2.do")
  @RequestMapping(value = "/comment/{blogId}", method = RequestMethod.POST)
  public ModelAndView testLogin2(String username, String password, int age) {
    // request和response不必非要出现在方法中，如果用不上的话可以去掉
    // 参数的名称是与页面控件的name相匹配，参数类型会自动被转换

    if (!"admin".equals(username) || !"admin".equals(password) || age < 5) {
      return new ModelAndView("loginError"); // 手动实例化ModelAndView完成跳转页面（转发），效果等同于上面的方法返回字符串
    }
    return new ModelAndView(new RedirectView("../index.jsp")); // 采用重定向方式跳转页面
    // 重定向还有一种简单写法
    // return new ModelAndView("redirect:../index.jsp");
  }

}