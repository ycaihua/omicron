package com.datagre.apps.omicron.biz.message;

import com.datagre.apps.omicron.biz.entity.ReleaseMessage;
import com.datagre.apps.omicron.biz.repository.ReleaseMessageRepository;
import com.datagre.apps.omicron.core.utils.OmicronThreadFactory;
import com.google.common.collect.Queues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by zengxiaobo on 2017/3/27.
 */
@Component
public class DatabaseMessageSender implements MessageSender {
  private static final Logger logger = LoggerFactory.getLogger(DatabaseMessageSender.class);
  private static final int CLEAN_QUEUE_MAX_SIZE = 100;
  private BlockingQueue<Long> toClean = Queues.newLinkedBlockingQueue(CLEAN_QUEUE_MAX_SIZE);
  private final ExecutorService cleanExecutorService;
  private final AtomicBoolean cleanStopped;

  @Autowired
  private ReleaseMessageRepository releaseMessageRepository;

  public DatabaseMessageSender() {
    cleanExecutorService = Executors.newSingleThreadExecutor(OmicronThreadFactory.create("DatabaseMessageSender", true));
    cleanStopped = new AtomicBoolean(false);
  }

  @Override
  @Transactional
  public void sendMessage(String message, String channel) {
    logger.info("Sending message {} to channel {}", message, channel);
    if (!Objects.equals(channel, Topics.OMICRON_RELEASE_TOPIC)) {
      logger.warn("Channel {} not supported by DatabaseMessageSender!");
      return;
    }

    try {
      ReleaseMessage newMessage = releaseMessageRepository.save(new ReleaseMessage(message));
      toClean.offer(newMessage.getId());
    } catch (Throwable ex) {
      logger.error("Sending message to database failed", ex);
    } finally {
    }
  }

  @PostConstruct
  private void initialize() {
    cleanExecutorService.submit(() -> {
      while (!cleanStopped.get() && !Thread.currentThread().isInterrupted()) {
        try {
          Long rm = toClean.poll(1, TimeUnit.SECONDS);
          if (rm != null) {
            cleanMessage(rm);
          } else {
            TimeUnit.SECONDS.sleep(5);
          }
        } catch (Throwable ex) {
        }
      }
    });
  }

  private void cleanMessage(Long id) {
    boolean hasMore = true;
    //double check in case the release message is rolled back
    ReleaseMessage releaseMessage = releaseMessageRepository.findOne(id);
    if (releaseMessage == null) {
      return;
    }
    while (hasMore && !Thread.currentThread().isInterrupted()) {
      List<ReleaseMessage> messages = releaseMessageRepository.findFirst100ByMessageAndIdLessThanOrderByIdAsc(
          releaseMessage.getMessage(), releaseMessage.getId());

      releaseMessageRepository.delete(messages);
      hasMore = messages.size() == 100;

      messages.forEach(toRemove -> logger.error(
          String.format("ReleaseMessage.Clean.%s", toRemove.getMessage()), String.valueOf(toRemove.getId())));
    }
  }

  void stopClean() {
    cleanStopped.set(true);
  }
}
