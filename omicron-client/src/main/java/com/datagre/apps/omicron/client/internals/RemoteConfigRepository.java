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
import com.datagre.apps.omicron.client.exceptions.OmicronConfigStatusCodeException;
import com.datagre.apps.omicron.client.util.ConfigUtil;
import com.datagre.apps.omicron.client.util.http.HttpRequest;
import com.datagre.apps.omicron.client.util.http.HttpResponse;
import com.datagre.apps.omicron.client.util.http.HttpUtil;
import com.datagre.apps.omicron.core.ConfigConsts;
import com.datagre.apps.omicron.core.dto.OmicronConfig;
import com.datagre.apps.omicron.core.dto.ServiceDTO;
import com.datagre.apps.omicron.core.utils.OmicronThreadFactory;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import com.google.common.util.concurrent.RateLimiter;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unidal.lookup.ContainerLoader;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by ycaihua on 2017/4/10.
 * https://github.com/ycaihua/omicron
 */
public class RemoteConfigRepository extends AbstractConfigRepository {
    private static final Logger logger = LoggerFactory.getLogger(RemoteConfigRepository.class);
    private static final Joiner STRING_JOINER = Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR);
    private static final Joiner.MapJoiner MAP_JOINER = Joiner.on("&").withKeyValueSeparator("=");
    private PlexusContainer m_container;
    private final ConfigServiceLocator serviceLocator;
    private final HttpUtil httpUtil;
    private final ConfigUtil configUtil;
    private final RemoteConfigLongPollService remoteConfigLongPollService;
    private volatile AtomicReference<OmicronConfig> configCache;
    private final String namespace;
    private final static ScheduledExecutorService executorService;
    private AtomicReference<ServiceDTO> longPollServiceDto;
    private RateLimiter loadConfigRateLimiter;
    private static final Escaper pathEscaper = UrlEscapers.urlPathSegmentEscaper();
    private static final Escaper queryParamEscaper = UrlEscapers.urlFormParameterEscaper();

    static {
        executorService = Executors.newScheduledThreadPool(1,
                OmicronThreadFactory.create("RemoteConfigRepository", true));
    }
    /**
     * Constructor.
     *
     * @param namespace the namespace
     */
    public RemoteConfigRepository(String namespace) {
        this.namespace = namespace;
        configCache = new AtomicReference<>();
        m_container = ContainerLoader.getDefaultContainer();
        try {
            configUtil = m_container.lookup(ConfigUtil.class);
            httpUtil = m_container.lookup(HttpUtil.class);
            serviceLocator = m_container.lookup(ConfigServiceLocator.class);
            remoteConfigLongPollService = m_container.lookup(RemoteConfigLongPollService.class);
        } catch (ComponentLookupException ex) {
            throw new OmicronConfigException("Unable to load component!", ex);
        }
        longPollServiceDto = new AtomicReference<>();
        loadConfigRateLimiter = RateLimiter.create(configUtil.getLoadConfigQPS());
        this.trySync();
        this.schedulePeriodicRefresh();
        this.scheduleLongPollingRefresh();
    }

    @Override
    public Properties getConfig() {
        if (configCache.get() == null) {
            this.sync();
        }
        return transformApolloConfigToProperties(configCache.get());
    }

    @Override
    public void setUpstreamRepository(ConfigRepository upstreamConfigRepository) {
        //remote config doesn't need upstream
    }

    private void schedulePeriodicRefresh() {
        logger.debug("Schedule periodic refresh with interval: {} {}",
                configUtil.getRefreshInterval(), configUtil.getRefreshIntervalTimeUnit());
        executorService.scheduleAtFixedRate(
                new Runnable() {
                    @Override
                    public void run() {
                        logger.debug("refresh config for namespace: {}", namespace);
                        trySync();
                    }
                }, configUtil.getRefreshInterval(), configUtil.getRefreshInterval(),
                configUtil.getRefreshIntervalTimeUnit());
    }

    @Override
    protected synchronized void sync() {
        try {
            OmicronConfig previous = configCache.get();
            OmicronConfig current = loadApolloConfig();
            //reference equals means HTTP 304
            if (previous != current) {
                logger.debug("Remote Config refreshed!");
                configCache.set(current);
                this.fireRepositoryChange(namespace, this.getConfig());
            }
        } catch (Throwable ex) {
            throw ex;
        }
    }

    private Properties transformApolloConfigToProperties(OmicronConfig apolloConfig) {
        Properties result = new Properties();
        result.putAll(apolloConfig.getConfigurations());
        return result;
    }

    private OmicronConfig loadApolloConfig() {
        if (!loadConfigRateLimiter.tryAcquire(5, TimeUnit.SECONDS)) {
            //wait at most 5 seconds
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
            }
        }
        String appId = configUtil.getAppId();
        String cluster = configUtil.getCluster();
        String dataCenter = configUtil.getDataCenter();
        int maxRetries = 2;
        Throwable exception = null;

        List<ServiceDTO> configServices = getConfigServices();
        for (int i = 0; i < maxRetries; i++) {
            List<ServiceDTO> randomConfigServices = Lists.newLinkedList(configServices);
            Collections.shuffle(randomConfigServices);
            //Access the server which notifies the client first
            if (longPollServiceDto.get() != null) {
                randomConfigServices.add(0, longPollServiceDto.getAndSet(null));
            }

            for (ServiceDTO configService : randomConfigServices) {
                String url =
                        assembleQueryConfigUrl(configService.getHomepageUrl(), appId, cluster, namespace,
                                dataCenter, configCache.get());

                logger.debug("Loading config from {}", url);
                HttpRequest request = new HttpRequest(url);
                try {

                    HttpResponse<OmicronConfig> response = httpUtil.doGet(request, OmicronConfig.class);
                    if (response.getStatusCode() == 304) {
                        logger.debug("Config server responds with 304 HTTP status code.");
                        return configCache.get();
                    }

                    OmicronConfig result = response.getBody();

                    logger.debug("Loaded config for {}: {}", namespace, result);

                    return result;
                } catch (OmicronConfigStatusCodeException ex) {
                    OmicronConfigStatusCodeException statusCodeException = ex;
                    //config not found
                    if (ex.getStatusCode() == 404) {
                        String message = String.format(
                                "Could not find config for namespace - appId: %s, cluster: %s, namespace: %s, " +
                                        "please check whether the configs are released in Apollo!",
                                appId, cluster, namespace);
                        statusCodeException = new OmicronConfigStatusCodeException(ex.getStatusCode(),
                                message);
                    }
                    exception = statusCodeException;
                } catch (Throwable ex) {
                    exception = ex;
                }
            }

            try {
                configUtil.getOnErrorRetryIntervalTimeUnit().sleep(configUtil.getOnErrorRetryInterval());
            } catch (InterruptedException ex) {
                //ignore
            }
        }
        String message = String.format(
                "Load Apollo Config failed - appId: %s, cluster: %s, namespace: %s",
                appId, cluster, namespace);
        throw new OmicronConfigException(message, exception);
    }

    String assembleQueryConfigUrl(String uri, String appId, String cluster, String namespace,
                                  String dataCenter, OmicronConfig previousConfig) {

        String path = "configs/%s/%s/%s";
        List<String> pathParams =
                Lists.newArrayList(pathEscaper.escape(appId), pathEscaper.escape(cluster),
                        pathEscaper.escape(namespace));
        Map<String, String> queryParams = Maps.newHashMap();

        if (previousConfig != null) {
            queryParams.put("releaseKey", queryParamEscaper.escape(previousConfig.getReleaseKey()));
        }

        if (!Strings.isNullOrEmpty(dataCenter)) {
            queryParams.put("dataCenter", queryParamEscaper.escape(dataCenter));
        }

        String localIp = configUtil.getLocalIp();
        if (!Strings.isNullOrEmpty(localIp)) {
            queryParams.put("ip", queryParamEscaper.escape(localIp));
        }

        String pathExpanded = String.format(path, pathParams.toArray());

        if (!queryParams.isEmpty()) {
            pathExpanded += "?" + MAP_JOINER.join(queryParams);
        }
        if (!uri.endsWith("/")) {
            uri += "/";
        }
        return uri + pathExpanded;
    }

    private void scheduleLongPollingRefresh() {
        remoteConfigLongPollService.submit(namespace, this);
    }

    public void onLongPollNotified(ServiceDTO longPollNotifiedServiceDto) {
        longPollServiceDto.set(longPollNotifiedServiceDto);
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                trySync();
            }
        });
    }

    private List<ServiceDTO> getConfigServices() {
        List<ServiceDTO> services = serviceLocator.getConfigServices();
        if (services.size() == 0) {
            throw new OmicronConfigException("No available config service");
        }

        return services;
    }
}
