package com.datagre.apps.omicron.biz.message;

import com.datagre.apps.omicron.biz.entity.ReleaseMessage;
import com.datagre.apps.omicron.biz.repository.ReleaseMessageRepository;
import com.datagre.apps.omicron.core.utils.OmicronThreadFactory;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by zengxiaobo on 2017/3/27.
 */
public class ReleaseMessageScanner implements InitializingBean {
  private static final Logger logger = LoggerFactory.getLogger(ReleaseMessageScanner.class);
  private static final int DEFAULT_SCAN_INTERVAL_IN_MS = 1000;
  @Autowired
  private Environment env;
  @Autowired
  private ReleaseMessageRepository releaseMessageRepository;
  private int databaseScanInterval;
  private List<ReleaseMessageListener> listeners;
  private ScheduledExecutorService executorService;
  private long maxIdScanned;

  public ReleaseMessageScanner() {
    listeners = Lists.newCopyOnWriteArrayList();
    executorService = Executors.newScheduledThreadPool(1, OmicronThreadFactory
        .create("ReleaseMessageScanner", true));
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    populateDataBaseInterval();
    maxIdScanned = loadLargestMessageId();
    executorService.scheduleWithFixedDelay((Runnable) () -> {
      try {
        scanMessages();
      } catch (Throwable ex) {
        logger.error("Scan and send message failed", ex);
      } finally {
      }
    }, getDatabaseScanIntervalMs(), getDatabaseScanIntervalMs(), TimeUnit.MILLISECONDS);

  }

  /**
   * add message listeners for release message
   * @param listener
   */
  public void addMessageListener(ReleaseMessageListener listener) {
    if (!listeners.contains(listener)) {
      listeners.add(listener);
    }
  }

  /**
   * Scan messages, continue scanning until there is no more messages
   */
  private void scanMessages() {
    boolean hasMoreMessages = true;
    while (hasMoreMessages && !Thread.currentThread().isInterrupted()) {
      hasMoreMessages = scanAndSendMessages();
    }
  }

  /**
   * scan messages and send
   *
   * @return whether there are more messages
   */
  private boolean scanAndSendMessages() {
    //current batch is 500
    List<ReleaseMessage> releaseMessages =
        releaseMessageRepository.findFirst500ByIdGreaterThanOrderByIdAsc(maxIdScanned);
    if (CollectionUtils.isEmpty(releaseMessages)) {
      return false;
    }
    fireMessageScanned(releaseMessages);
    int messageScanned = releaseMessages.size();
    maxIdScanned = releaseMessages.get(messageScanned - 1).getId();
    return messageScanned == 500;
  }

  /**
   * find largest message id as the current start point
   * @return current largest message id
   */
  private long loadLargestMessageId() {
    ReleaseMessage releaseMessage = releaseMessageRepository.findTopByOrderByIdDesc();
    return releaseMessage == null ? 0 : releaseMessage.getId();
  }

  /**
   * Notify listeners with messages loaded
   * @param messages
   */
  private void fireMessageScanned(List<ReleaseMessage> messages) {
    for (ReleaseMessage message : messages) {
      for (ReleaseMessageListener listener : listeners) {
        try {
          listener.handleMessage(message, Topics.OMICRON_RELEASE_TOPIC);
        } catch (Throwable ex) {
          logger.error("Failed to invoke message listener {}", listener.getClass(), ex);
        }
      }
    }
  }

  private void populateDataBaseInterval() {
    databaseScanInterval = DEFAULT_SCAN_INTERVAL_IN_MS;
    try {
      String interval = env.getProperty("apollo.message-scan.interval");
      if (!Objects.isNull(interval)) {
        databaseScanInterval = Integer.parseInt(interval);
      }
    } catch (Throwable ex) {
      logger.error("Load apollo message scan interval from system property failed", ex);
    }
  }

  private int getDatabaseScanIntervalMs() {
    return databaseScanInterval;
  }
}
