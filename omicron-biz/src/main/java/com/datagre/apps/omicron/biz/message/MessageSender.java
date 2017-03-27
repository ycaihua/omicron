package com.datagre.apps.omicron.biz.message;

/**
 * Created by zengxiaobo on 2017/3/27.
 */
public interface MessageSender {
  void sendMessage(String message, String channel);
}
