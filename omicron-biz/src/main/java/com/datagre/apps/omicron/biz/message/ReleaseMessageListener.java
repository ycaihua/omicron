package com.datagre.apps.omicron.biz.message;

import com.datagre.apps.omicron.biz.entity.ReleaseMessage;

/**
 * Created by zengxiaobo on 2017/3/27.
 */
public interface ReleaseMessageListener {
  void handleMessage(ReleaseMessage message, String channel);
}
