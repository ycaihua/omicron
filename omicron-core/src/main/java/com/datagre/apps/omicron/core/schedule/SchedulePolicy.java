package com.datagre.apps.omicron.core.schedule;

/**
 * Created by zengxiaobo on 2017/3/24.
 */
public interface SchedulePolicy {
  long fail();
  void success();
}
