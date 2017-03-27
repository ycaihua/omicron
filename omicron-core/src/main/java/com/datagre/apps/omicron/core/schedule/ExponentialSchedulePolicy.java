package com.datagre.apps.omicron.core.schedule;

/**
 * Created by zengxiaobo on 2017/3/24.
 */
public class ExponentialSchedulePolicy implements SchedulePolicy {
  private final long delayTimeLowerBound;
  private final long delayTimeUpperBound;
  private long lastDelayTime;

  public ExponentialSchedulePolicy(long delayTimeLowerBound, long delayTimeUpperBound) {
    this.delayTimeLowerBound = delayTimeLowerBound;
    this.delayTimeUpperBound = delayTimeUpperBound;
  }

  @Override
  public long fail() {
    long delayTime = lastDelayTime;

    if (delayTime == 0) {
      delayTime = delayTimeLowerBound;
    } else {
      delayTime = Math.min(lastDelayTime << 1, delayTimeUpperBound);
    }

    lastDelayTime = delayTime;

    return delayTime;
  }

  @Override
  public void success() {
    lastDelayTime = 0;
  }
}
