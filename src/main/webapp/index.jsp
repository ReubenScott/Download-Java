<?xml version="1.0" encoding="UTF-8" ?>
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>Insert title here</title>
</head>
<body>


  <form name="formLogin" action="download/m3u8.htm"  method="post">
    <div class="tip">
      <input class="userName" name="url" type="text" style="width:800px;"/>
      <input type="submit" value="下載"   />
    </div>
  </form>

  <form name="downLogin" action="download/download.htm"  method="post">
    <div class="tip">
      <input  name="filename" type="text" style="width:800px;"/>
      <input type="submit" value="下載"   />
    </div>
  </form>
  
</body>
</html>