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

import com.datagre.apps.omicron.biz.entity.ReleaseMessage;
import com.datagre.apps.omicron.biz.message.ReleaseMessageListener;
import com.datagre.apps.omicron.biz.message.Topics;
import com.datagre.apps.omicron.biz.utils.EntityManagerUtil;
import com.datagre.apps.omicron.config.service.ReleaseMessageCacheService;
import com.datagre.apps.omicron.config.util.NamespaceUtil;
import com.datagre.apps.omicron.config.util.WatchKeysUtil;
import com.datagre.apps.omicron.core.ConfigConsts;
import com.datagre.apps.omicron.core.dto.OmicronConfigNotification;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Set;

/**
 * Created by ycaihua on 2017/4/6.
 * https://github.com/ycaihua/omicron
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController implements ReleaseMessageListener{
    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);
    private static final long TIMEOUT = 30 * 1000;//30 seconds
    private final Multimap<String, DeferredResult<ResponseEntity<OmicronConfigNotification>>>
            deferredResults = Multimaps.synchronizedSetMultimap(HashMultimap.create());
    private static final ResponseEntity<OmicronConfigNotification>
            NOT_MODIFIED_RESPONSE = new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
    private static final Splitter STRING_SPLITTER =
            Splitter.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR).omitEmptyStrings();

    @Autowired
    private WatchKeysUtil watchKeysUtil;

    @Autowired
    private ReleaseMessageCacheService releaseMessageService;

    @Autowired
    private EntityManagerUtil entityManagerUtil;

    @Autowired
    private NamespaceUtil namespaceUtil;
    /**
     * For single namespace notification, reserved for older version of apollo clients
     *
     * @param appId          the appId
     * @param cluster        the cluster
     * @param namespace      the namespace name
     * @param dataCenter     the datacenter
     * @param notificationId the notification id for the namespace
     * @param clientIp       the client side ip
     * @return a deferred result
     */
    @RequestMapping(method = RequestMethod.GET)
    public DeferredResult<ResponseEntity<OmicronConfigNotification>> pollNotification(
            @RequestParam(value = "appId") String appId,
            @RequestParam(value = "cluster") String cluster,
            @RequestParam(value = "namespace", defaultValue = ConfigConsts.NAMESPACE_APPLICATION) String namespace,
            @RequestParam(value = "dataCenter", required = false) String dataCenter,
            @RequestParam(value = "notificationId", defaultValue = "-1") long notificationId,
            @RequestParam(value = "ip", required = false) String clientIp) {
        //strip out .properties suffix
        namespace = namespaceUtil.filterNamespaceName(namespace);

        Set<String> watchedKeys = watchKeysUtil.assembleAllWatchKeys(appId, cluster, namespace, dataCenter);

        DeferredResult<ResponseEntity<OmicronConfigNotification>> deferredResult =
                new DeferredResult<>(TIMEOUT, NOT_MODIFIED_RESPONSE);

        //check whether client is out-dated
        ReleaseMessage latest = releaseMessageService.findLatestReleaseMessageForMessages(watchedKeys);

        /**
         * Manually close the entity manager.
         * Since for async request, Spring won't do so until the request is finished,
         * which is unacceptable since we are doing long polling - means the db connection would be hold
         * for a very long time
         */
        entityManagerUtil.closeEntityManager();

        if (latest != null && latest.getId() != notificationId) {
            deferredResult.setResult(new ResponseEntity<>(
                    new OmicronConfigNotification(namespace, latest.getId()), HttpStatus.OK));
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
                    watchedKeys, appId, cluster, namespace, dataCenter);
        }
        return deferredResult;
    }
    @Override
    public void handleMessage(ReleaseMessage message, String channel) {
        logger.info("message received - channel: {}, message: {}", channel, message);

        String content = message.getMessage();
        if (!Topics.OMICRON_RELEASE_TOPIC.equals(channel) || Strings.isNullOrEmpty(content)) {
            return;
        }
        List<String> keys = STRING_SPLITTER.splitToList(content);
        //message should be appId+cluster+namespace
        if (keys.size() != 3) {
            logger.error("message format invalid - {}", content);
            return;
        }
        ResponseEntity<OmicronConfigNotification> notification =
                new ResponseEntity<>(
                        new OmicronConfigNotification(keys.get(2), message.getId()), HttpStatus.OK);
        if (!deferredResults.containsKey(content)) {
            return;
        }
        //create a new list to avoid ConcurrentModificationException
        List<DeferredResult<ResponseEntity<OmicronConfigNotification>>> results =
                Lists.newArrayList(deferredResults.get(content));
        logger.debug("Notify {} clients for key {}", results.size(), content);

        for (DeferredResult<ResponseEntity<OmicronConfigNotification>> result : results) {
            result.setResult(notification);
        }
        logger.debug("Notification completed");
    }
}
