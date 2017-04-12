/**
 * Copyright 2016-2017 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.datagre.apps.omicron.config.controller;

import com.datagre.apps.omicron.biz.config.BizConfig;
import com.datagre.apps.omicron.biz.entity.ReleaseMessage;
import com.datagre.apps.omicron.biz.message.ReleaseMessageListener;
import com.datagre.apps.omicron.biz.message.Topics;
import com.datagre.apps.omicron.biz.utils.EntityManagerUtil;
import com.datagre.apps.omicron.common.exception.BadRequestException;
import com.datagre.apps.omicron.config.service.ReleaseMessageCacheService;
import com.datagre.apps.omicron.config.util.NamespaceUtil;
import com.datagre.apps.omicron.config.util.WatchKeysUtil;
import com.datagre.apps.omicron.core.ConfigConsts;
import com.datagre.apps.omicron.core.dto.OmicronConfigNotification;
import com.datagre.apps.omicron.core.utils.OmicronThreadFactory;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by ycaihua on 2017/4/6.
 * https://github.com/ycaihua/omicron
 */
@RestController
@RequestMapping("/notifications/v2")
public class NotificationControllerV2 implements ReleaseMessageListener {
    private static final Logger logger = LoggerFactory.getLogger(NotificationControllerV2.class);
    private static final long TIMEOUT = 30 * 1000;//30 seconds
    private final Multimap<String, DeferredResult<ResponseEntity<List<OmicronConfigNotification>>>>
            deferredResults = Multimaps.synchronizedSetMultimap(HashMultimap.create());
    private static final ResponseEntity<List<OmicronConfigNotification>>
            NOT_MODIFIED_RESPONSE_LIST = new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
    private static final Splitter STRING_SPLITTER =
            Splitter.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR).omitEmptyStrings();
    private static final long NOTIFICATION_ID_PLACEHOLDER = -1;
    private static final Type notificationsTypeReference =
            new TypeToken<List<OmicronConfigNotification>>() {
            }.getType();

    private final ExecutorService largeNotificationBatchExecutorService;

    @Autowired
    private WatchKeysUtil watchKeysUtil;

    @Autowired
    private ReleaseMessageCacheService releaseMessageService;

    @Autowired
    private EntityManagerUtil entityManagerUtil;

    @Autowired
    private NamespaceUtil namespaceUtil;

    @Autowired
    private Gson gson;

    @Autowired
    private BizConfig bizConfig;

    public NotificationControllerV2() {
        largeNotificationBatchExecutorService = Executors.newSingleThreadExecutor(OmicronThreadFactory.create
                ("NotificationControllerV2", true));
    }

    @RequestMapping(method = RequestMethod.GET)
    public DeferredResult<ResponseEntity<List<OmicronConfigNotification>>> pollNotification(
            @RequestParam(value = "appId") String appId,
            @RequestParam(value = "cluster") String cluster,
            @RequestParam(value = "notifications") String notificationsAsString,
            @RequestParam(value = "dataCenter", required = false) String dataCenter,
            @RequestParam(value = "ip", required = false) String clientIp) {
        List<OmicronConfigNotification> notifications = null;

        try {
            notifications =
                    gson.fromJson(notificationsAsString, notificationsTypeReference);
        } catch (Throwable ex) {
            logger.info(ex.getMessage());
        }

        if (CollectionUtils.isEmpty(notifications)) {
            throw new BadRequestException("Invalid format of notifications: " + notificationsAsString);
        }

        Set<String> namespaces = Sets.newHashSet();
        Map<String, Long> clientSideNotifications = Maps.newHashMap();
        for (OmicronConfigNotification notification : notifications) {
            if (Strings.isNullOrEmpty(notification.getNamespaceName())) {
                continue;
            }
            //strip out .properties suffix
            String namespace = namespaceUtil.filterNamespaceName(notification.getNamespaceName());
            namespaces.add(namespace);
            clientSideNotifications.put(namespace, notification.getNotificationId());
        }

        if (CollectionUtils.isEmpty(namespaces)) {
            throw new BadRequestException("Invalid format of notifications: " + notificationsAsString);
        }

        Multimap<String, String> watchedKeysMap =
                watchKeysUtil.assembleAllWatchKeys(appId, cluster, namespaces, dataCenter);

        DeferredResult<ResponseEntity<List<OmicronConfigNotification>>> deferredResult =
                new DeferredResult<>(TIMEOUT, NOT_MODIFIED_RESPONSE_LIST);

        Set<String> watchedKeys = Sets.newHashSet(watchedKeysMap.values());

        List<ReleaseMessage> latestReleaseMessages =
                releaseMessageService.findLatestReleaseMessagesGroupByMessages(watchedKeys);

        /**
         * Manually close the entity manager.
         * Since for async request, Spring won't do so until the request is finished,
         * which is unacceptable since we are doing long polling - means the db connection would be hold
         * for a very long time
         */
        entityManagerUtil.closeEntityManager();

        List<OmicronConfigNotification> newNotifications =
                getApolloConfigNotifications(namespaces, clientSideNotifications, watchedKeysMap,
                        latestReleaseMessages);

        if (!CollectionUtils.isEmpty(newNotifications)) {
            deferredResult.setResult(new ResponseEntity<>(newNotifications, HttpStatus.OK));
        } else {
            //register all keys
            for (String key : watchedKeys) {
                this.deferredResults.put(key, deferredResult);
            }
            deferredResult.onCompletion(() -> {
                //unregister all keys
                for (String key : watchedKeys) {
                    deferredResults.remove(key, deferredResult);
                }
            });
            logger.debug("Listening {} from appId: {}, cluster: {}, namespace: {}, datacenter: {}",
                    watchedKeys, appId, cluster, namespaces, dataCenter);
        }

        return deferredResult;
    }

    private List<OmicronConfigNotification> getApolloConfigNotifications(Set<String> namespaces,
                                                                        Map<String, Long> clientSideNotifications,
                                                                        Multimap<String, String> watchedKeysMap,
                                                                        List<ReleaseMessage> latestReleaseMessages) {
        List<OmicronConfigNotification> newNotifications = Lists.newArrayList();
        if (!CollectionUtils.isEmpty(latestReleaseMessages)) {
            Map<String, Long> latestNotifications = Maps.newHashMap();
            for (ReleaseMessage releaseMessage : latestReleaseMessages) {
                latestNotifications.put(releaseMessage.getMessage(), releaseMessage.getId());
            }

            for (String namespace : namespaces) {
                long clientSideId = clientSideNotifications.get(namespace);
                long latestId = NOTIFICATION_ID_PLACEHOLDER;
                Collection<String> namespaceWatchedKeys = watchedKeysMap.get(namespace);
                for (String namespaceWatchedKey : namespaceWatchedKeys) {
                    long namespaceNotificationId =
                            latestNotifications.getOrDefault(namespaceWatchedKey, NOTIFICATION_ID_PLACEHOLDER);
                    if (namespaceNotificationId > latestId) {
                        latestId = namespaceNotificationId;
                    }
                }
                if (latestId > clientSideId) {
                    newNotifications.add(new OmicronConfigNotification(namespace, latestId));
                }
            }
        }
        return newNotifications;
    }

    @Override
    public void handleMessage(ReleaseMessage message, String channel) {
        logger.info("message received - channel: {}, message: {}", channel, message);

        String content = message.getMessage();
        if (!Topics.OMICRON_RELEASE_TOPIC.equals(channel) || Strings.isNullOrEmpty(content)) {
            return;
        }

        String changedNamespace = retrieveNamespaceFromReleaseMessage.apply(content);

        if (Strings.isNullOrEmpty(changedNamespace)) {
            logger.error("message format invalid - {}", content);
            return;
        }

        ResponseEntity<List<OmicronConfigNotification>> notification =
                new ResponseEntity<>(
                        Lists.newArrayList(new OmicronConfigNotification(changedNamespace, message.getId())),
                        HttpStatus.OK);

        if (!deferredResults.containsKey(content)) {
            return;
        }
        //create a new list to avoid ConcurrentModificationException
        List<DeferredResult<ResponseEntity<List<OmicronConfigNotification>>>> results =
                Lists.newArrayList(deferredResults.get(content));

        //do async notification if too many clients
        if (results.size() > bizConfig.releaseMessageNotificationBatch()) {
            largeNotificationBatchExecutorService.submit(() -> {
                logger.debug("Async notify {} clients for key {} with batch {}", results.size(), content,
                        bizConfig.releaseMessageNotificationBatch());
                for (int i = 0; i < results.size(); i++) {
                    if (i > 0 && i % bizConfig.releaseMessageNotificationBatch() == 0) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(bizConfig.releaseMessageNotificationBatchIntervalInMilli());
                        } catch (InterruptedException e) {
                            //ignore
                        }
                    }
                    logger.debug("Async notify {}", results.get(i));
                    results.get(i).setResult(notification);
                }
            });
            return;
        }

        logger.debug("Notify {} clients for key {}", results.size(), content);

        for (DeferredResult<ResponseEntity<List<OmicronConfigNotification>>> result : results) {
            result.setResult(notification);
        }
        logger.debug("Notification completed");
    }

    private static final Function<String, String> retrieveNamespaceFromReleaseMessage =
            releaseMessage -> {
                if (Strings.isNullOrEmpty(releaseMessage)) {
                    return null;
                }
                List<String> keys = STRING_SPLITTER.splitToList(releaseMessage);
                //message should be appId+cluster+namespace
                if (keys.size() != 3) {
                    logger.error("message format invalid - {}", releaseMessage);
                    return null;
                }
                return keys.get(2);
            };
}
