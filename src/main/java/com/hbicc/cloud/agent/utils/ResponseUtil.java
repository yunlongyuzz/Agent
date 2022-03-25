package com.hbicc.cloud.agent.utils;
import lombok.Data;

@Data
public class ResponseUtil {
  private Integer code;
  private String message;
  private Object data;

  public static ResponseUtil out(Object o) {
    if (null == o) {
      return error(0, "", o);
    }
    return success(1, "", o);
  }

  public static ResponseUtil success(Integer i, String m, Object o) {
    ResponseUtil response = new ResponseUtil();
    response.setCode(i);
    if ("".equals(m)) {
      m = "SUCCESS";
    }
    response.setMessage(m);
    response.setData(o);
    return response;
  }

  public static ResponseUtil error(Integer i, String m, Object o) {
    ResponseUtil response = new ResponseUtil();
    response.setCode(i);
    if ("".equals(m)) {
      m = "ERROR";
    }
    response.setMessage(m);
    response.setData(o);
    return response;
  }
}
