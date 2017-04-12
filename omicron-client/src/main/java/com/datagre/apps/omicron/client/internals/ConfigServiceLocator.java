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
import com.datagre.apps.omicron.client.util.http.HttpRequest;
import com.datagre.apps.omicron.client.util.http.HttpResponse;
import com.datagre.apps.omicron.client.util.http.HttpUtil;
import com.datagre.apps.omicron.core.dto.ServiceDTO;
import com.datagre.apps.omicron.core.utils.OmicronThreadFactory;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.escape.Escaper;
import com.google.common.net.UrlEscapers;
import com.google.common.reflect.TypeToken;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unidal.lookup.annotation.Inject;
import org.unidal.lookup.annotation.Named;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by ycaihua on 2017/4/7.
 * https://github.com/ycaihua/omicron
 */
@Named(type = ConfigServiceLocator.class)
public class ConfigServiceLocator implements Initializable {
    private static final Logger logger = LoggerFactory.getLogger(ConfigServiceLocator.class);
    @Inject
    private HttpUtil httpUtil;
    @Inject
    private ConfigUtil configUtil;
    private AtomicReference<List<ServiceDTO>> configServices;
    private Type responseType;
    private ScheduledExecutorService executorService;
    private static final Joiner.MapJoiner MAP_JOINER =  Joiner.on("&").withKeyValueSeparator("=");
    private static final Escaper queryParamEscaper = UrlEscapers.urlFragmentEscaper();

    public ConfigServiceLocator() {
        List<ServiceDTO> initial = Lists.newArrayList();
        configServices = new AtomicReference<>(initial);
        responseType =  new TypeToken<List<ServiceDTO>>(){}.getType();
        this.executorService = Executors.newScheduledThreadPool(1, OmicronThreadFactory.create("ConfigServiceLocator",true));
    }

    @Override
    public void initialize() throws InitializationException {
        this.tryUpdateConfigServices();
        this.schedulePeriodicRefresh();
    }
    /**
     * Get the config service info from remote meta server.
     *
     * @return the services dto
     */
    public List<ServiceDTO> getConfigServices() {
        if (configServices.get().isEmpty()) {
            updateConfigServices();
        }

        return configServices.get();
    }
    private void schedulePeriodicRefresh() {
        this.executorService.scheduleAtFixedRate(()->{
            logger.debug("refresh config services");
            tryUpdateConfigServices();
        },configUtil.getRefreshInterval(),configUtil.getRefreshInterval(),configUtil.getRefreshIntervalTimeUnit());
    }

    private boolean tryUpdateConfigServices() {
        try {
            updateConfigServices();
            return true;
        } catch (Throwable ex) {
            //ignore
        }
        return false;
    }

    private synchronized void updateConfigServices() {
        String url = assembleMetaServiceUrl();

        HttpRequest request = new HttpRequest(url);
        int maxRetries = 5;
        Throwable exception = null;

        for (int i = 0; i < maxRetries; i++) {
            try {
                HttpResponse<List<ServiceDTO>> response = httpUtil.doGet(request, responseType);
                List<ServiceDTO> services = response.getBody();
                if (services == null || services.isEmpty()) {
                    continue;
                }
                configServices.set(services);
                return;
            } catch (Throwable ex) {
                exception = ex;
            }

            try {
                configUtil.getOnErrorRetryIntervalTimeUnit().sleep(configUtil.getOnErrorRetryInterval());
            } catch (InterruptedException ex) {
                //ignore
            }
        }

        throw new OmicronConfigException(
                String.format("Get config services failed from %s", url), exception);
    }
    private String assembleMetaServiceUrl() {
        String domainName = configUtil.getMetaServerDomainName();
        String appId = configUtil.getAppId();
        String localIp = configUtil.getLocalIp();

        Map<String, String> queryParams = Maps.newHashMap();
        queryParams.put("appId", queryParamEscaper.escape(appId));
        if (!Strings.isNullOrEmpty(localIp)) {
            queryParams.put("ip", queryParamEscaper.escape(localIp));
        }

        return domainName + "/services/config?" + MAP_JOINER.join(queryParams);
    }
}
