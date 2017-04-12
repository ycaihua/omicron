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
package com.datagre.apps.omicron.client.internals;

import com.datagre.apps.omicron.client.exceptions.OmicronConfigException;
import com.datagre.apps.omicron.client.util.ConfigUtil;
import com.datagre.apps.omicron.client.util.ExceptionUtil;
import com.datagre.apps.omicron.client.util.http.HttpRequest;
import com.datagre.apps.omicron.client.util.http.HttpResponse;
import com.datagre.apps.omicron.client.util.http.HttpUtil;
import com.datagre.apps.omicron.core.ConfigConsts;
import com.datagre.apps.omicron.core.dto.OmicronConfigNotification;
import com.datagre.apps.omicron.core.dto.ServiceDTO;
import com.datagre.apps.omicron.core.enums.ConfigFileFormat;
import com.datagre.apps.omicron.core.schedule.ExponentialSchedulePolicy;
import com.datagre.apps.omicron.core.schedule.SchedulePolicy;
import com.datagre.apps.omicron.core.utils.OmicronThreadFactory;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.*;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.RateLimiter;
import com.google.gson.Gson;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unidal.lookup.annotation.Inject;
import org.unidal.lookup.annotation.Named;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by ycaihua on 2017/4/10.
 * https://github.com/ycaihua/omicron
 */
@Named(type = RemoteConfigLongPollService.class)
public class RemoteConfigLongPollService implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(RemoteConfigLongPollService.class);
    private static final Joiner STRING_JOINER = Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR);
    private static final Joiner.MapJoiner MAP_JOINER = Joiner.on("&").withKeyValueSeparator("=");
    private static final Escaper queryParamEscaper = UrlEscapers.urlFormParameterEscaper();
    private static final long INIT_NOTIFICATION_ID = -1;
    private final ExecutorService m_longPollingService;
    private final AtomicBoolean m_longPollingStopped;
    private SchedulePolicy m_longPollFailSchedulePolicyInSecond;
    private RateLimiter m_longPollRateLimiter;
    private final AtomicBoolean m_longPollStarted;
    private final Multimap<String, RemoteConfigRepository> m_longPollNamespaces;
    private final ConcurrentMap<String, Long> m_notifications;
    private Type m_responseType;
    private Gson gson;
    @Inject
    private ConfigUtil m_configUtil;
    @Inject
    private HttpUtil m_httpUtil;
    @Inject
    private ConfigServiceLocator m_serviceLocator;

    public RemoteConfigLongPollService() {
        m_longPollFailSchedulePolicyInSecond = new ExponentialSchedulePolicy(1, 120); //in second
        m_longPollingStopped = new AtomicBoolean(false);
        m_longPollingService = Executors.newSingleThreadExecutor(
                OmicronThreadFactory.create("RemoteConfigLongPollService", true));
        m_longPollStarted = new AtomicBoolean(false);
        m_longPollNamespaces =
                Multimaps.synchronizedSetMultimap(HashMultimap.<String, RemoteConfigRepository>create());
        m_notifications = Maps.newConcurrentMap();
        m_responseType = new TypeToken<List<OmicronConfigNotification>>() {
        }.getType();
        gson = new Gson();
    }

    @Override
    public void initialize() throws InitializationException {
        m_longPollRateLimiter = RateLimiter.create(m_configUtil.getLongPollQPS());
    }
    public boolean submit(String namespace, RemoteConfigRepository remoteConfigRepository) {
        boolean added = m_longPollNamespaces.put(namespace, remoteConfigRepository);
        m_notifications.putIfAbsent(namespace, INIT_NOTIFICATION_ID);
        if (!m_longPollStarted.get()) {
            startLongPolling();
        }
        return added;
    }

    private void startLongPolling() {
        if (!m_longPollStarted.compareAndSet(false, true)) {
            //already started
            return;
        }
        try {
            final String appId = m_configUtil.getAppId();
            final String cluster = m_configUtil.getCluster();
            final String dataCenter = m_configUtil.getDataCenter();
            m_longPollingService.submit(new Runnable() {
                @Override
                public void run() {
                    doLongPollingRefresh(appId, cluster, dataCenter);
                }
            });
        } catch (Throwable ex) {
            m_longPollStarted.set(false);
            OmicronConfigException exception = new OmicronConfigException("Schedule long polling refresh failed", ex);
            logger.warn(ExceptionUtil.getDetailMessage(exception));
        }
    }

    void stopLongPollingRefresh() {
        this.m_longPollingStopped.compareAndSet(false, true);
    }

    private void doLongPollingRefresh(String appId, String cluster, String dataCenter) {
        final Random random = new Random();
        ServiceDTO lastServiceDto = null;
        while (!m_longPollingStopped.get() && !Thread.currentThread().isInterrupted()) {
            if (!m_longPollRateLimiter.tryAcquire(5, TimeUnit.SECONDS)) {
                //wait at most 5 seconds
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException e) {
                }
            }
            try {
                if (lastServiceDto == null) {
                    List<ServiceDTO> configServices = getConfigServices();
                    lastServiceDto = configServices.get(random.nextInt(configServices.size()));
                }

                String url =
                        assembleLongPollRefreshUrl(lastServiceDto.getHomepageUrl(), appId, cluster, dataCenter,
                                m_notifications);

                logger.debug("Long polling from {}", url);
                HttpRequest request = new HttpRequest(url);
                //longer timeout for read - 10 minutes
                request.setReadTimeout(600000);

                final HttpResponse<List<OmicronConfigNotification>> response =
                        m_httpUtil.doGet(request, m_responseType);

                logger.debug("Long polling response: {}, url: {}", response.getStatusCode(), url);
                if (response.getStatusCode() == 200 && response.getBody() != null) {
                    updateNotifications(response.getBody());
                    notify(lastServiceDto, response.getBody());
                }

                //try to load balance
                if (response.getStatusCode() == 304 && random.nextBoolean()) {
                    lastServiceDto = null;
                }

                m_longPollFailSchedulePolicyInSecond.success();
            } catch (Throwable ex) {
                lastServiceDto = null;
                long sleepTimeInSecond = m_longPollFailSchedulePolicyInSecond.fail();
                logger.warn(
                        "Long polling failed, will retry in {} seconds. appId: {}, cluster: {}, namespaces: {}, reason: {}",
                        sleepTimeInSecond, appId, cluster, assembleNamespaces(),
                        ExceptionUtil.getDetailMessage(ex));
                try {
                    TimeUnit.SECONDS.sleep(sleepTimeInSecond);
                } catch (InterruptedException ie) {
                    //ignore
                }
            }
        }
    }

    private void notify(ServiceDTO lastServiceDto, List<OmicronConfigNotification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return;
        }
        for (OmicronConfigNotification notification : notifications) {
            String namespaceName = notification.getNamespaceName();
            //create a new list to avoid ConcurrentModificationException
            List<RemoteConfigRepository> toBeNotified =
                    Lists.newArrayList(m_longPollNamespaces.get(namespaceName));
            //since .properties are filtered out by default, so we need to check if there is any listener for it
            toBeNotified.addAll(m_longPollNamespaces
                    .get(String.format("%s.%s", namespaceName, ConfigFileFormat.Properties.getValue())));
            for (RemoteConfigRepository remoteConfigRepository : toBeNotified) {
                try {
                    remoteConfigRepository.onLongPollNotified(lastServiceDto);
                } catch (Throwable ex) {
                }
            }
        }
    }

    private void updateNotifications(List<OmicronConfigNotification> deltaNotifications) {
        for (OmicronConfigNotification notification : deltaNotifications) {
            if (Strings.isNullOrEmpty(notification.getNamespaceName())) {
                continue;
            }
            String namespaceName = notification.getNamespaceName();
            if (m_notifications.containsKey(namespaceName)) {
                m_notifications.put(namespaceName, notification.getNotificationId());
            }
            //since .properties are filtered out by default, so we need to check if there is notification with .properties suffix
            String namespaceNameWithPropertiesSuffix =
                    String.format("%s.%s", namespaceName, ConfigFileFormat.Properties.getValue());
            if (m_notifications.containsKey(namespaceNameWithPropertiesSuffix)) {
                m_notifications.put(namespaceNameWithPropertiesSuffix, notification.getNotificationId());
            }
        }
    }

    private String assembleNamespaces() {
        return STRING_JOINER.join(m_longPollNamespaces.keySet());
    }

    String assembleLongPollRefreshUrl(String uri, String appId, String cluster, String dataCenter,
                                      Map<String, Long> notificationsMap) {
        Map<String, String> queryParams = Maps.newHashMap();
        queryParams.put("appId", queryParamEscaper.escape(appId));
        queryParams.put("cluster", queryParamEscaper.escape(cluster));
        queryParams
                .put("notifications", queryParamEscaper.escape(assembleNotifications(notificationsMap)));

        if (!Strings.isNullOrEmpty(dataCenter)) {
            queryParams.put("dataCenter", queryParamEscaper.escape(dataCenter));
        }
        String localIp = m_configUtil.getLocalIp();
        if (!Strings.isNullOrEmpty(localIp)) {
            queryParams.put("ip", queryParamEscaper.escape(localIp));
        }

        String params = MAP_JOINER.join(queryParams);
        if (!uri.endsWith("/")) {
            uri += "/";
        }

        return uri + "notifications/v2?" + params;
    }

    String assembleNotifications(Map<String, Long> notificationsMap) {
        List<OmicronConfigNotification> notifications = Lists.newArrayList();
        for (Map.Entry<String, Long> entry : notificationsMap.entrySet()) {
            OmicronConfigNotification notification = new OmicronConfigNotification();
            notification.setNamespaceName(entry.getKey());
            notification.setNotificationId(entry.getValue());
            notifications.add(notification);
        }
        return gson.toJson(notifications);
    }

    private List<ServiceDTO> getConfigServices() {
        List<ServiceDTO> services = m_serviceLocator.getConfigServices();
        if (services.size() == 0) {
            throw new OmicronConfigException("No available config service");
        }

        return services;
    }
}
