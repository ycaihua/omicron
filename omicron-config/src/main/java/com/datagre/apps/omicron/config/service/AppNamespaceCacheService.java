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
package com.datagre.apps.omicron.config.service;

import com.datagre.apps.omicron.biz.config.BizConfig;
import com.datagre.apps.omicron.biz.repository.AppNamespaceRepository;
import com.datagre.apps.omicron.common.entity.AppNamespace;
import com.datagre.apps.omicron.core.ConfigConsts;
import com.datagre.apps.omicron.core.utils.OmicronThreadFactory;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.apachecommons.CommonsLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by ycaihua on 2017/4/6.
 * https://github.com/ycaihua/omicron
 */
@Service
public class AppNamespaceCacheService implements InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(AppNamespaceCacheService.class);
    private static final Joiner STRING_JOINER=Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR).skipNulls();
    @Autowired
    private AppNamespaceRepository appNamespaceRepository;
    @Autowired
    private BizConfig bizConfig;
    private int scanInterval;
    private int rebuildInterval;
    private TimeUnit scanIntervalTimeUnit;
    private TimeUnit rebuildIntervalTimeUnit;
    private ScheduledExecutorService scheduledExecutorService;
    private long maxIdScanned;
    private Map<String,AppNamespace> publicAppNamespaceCache;
    private Map<String,AppNamespace> appNamespaceCache;
    private Map<Long,AppNamespace> appNamespaceIdCache;

    public AppNamespaceCacheService() {
        maxIdScanned=0;
        publicAppNamespaceCache= Maps.newConcurrentMap();
        appNamespaceCache=Maps.newConcurrentMap();
        appNamespaceIdCache=Maps.newConcurrentMap();
        scheduledExecutorService= Executors.newScheduledThreadPool(1, OmicronThreadFactory.create("AppNamespaceCacheService",true));
    }
    public List<AppNamespace> findByAppIdAndNamespaces(String appId, Set<String> namespaceNames) {
        Preconditions.checkArgument(!Strings.isNullOrEmpty(appId), "appId must not be null");
        if (namespaceNames == null || namespaceNames.isEmpty()) {
            return Collections.emptyList();
        }
        List<AppNamespace> result = Lists.newArrayList();
        for (String namespaceName : namespaceNames) {
            AppNamespace appNamespace = appNamespaceCache.get(STRING_JOINER.join(appId, namespaceName));
            if (appNamespace != null) {
                result.add(appNamespace);
            }
        }
        return result;
    }

    public List<AppNamespace> findPublicNamespacesByNames(Set<String> namespaceNames) {
        if (namespaceNames == null || namespaceNames.isEmpty()) {
            return Collections.emptyList();
        }
        List<AppNamespace> result = Lists.newArrayList();
        for (String namespaceName : namespaceNames) {
            AppNamespace appNamespace = publicAppNamespaceCache.get(namespaceName);
            if (appNamespace != null) {
                result.add(appNamespace);
            }
        }
        return result;
    }
    @Override
    public void afterPropertiesSet() throws Exception {
        populateDataBaseInterval();
        scanNewAppNamespaces(); //block the startup process until load finished
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                this.updateAndDeleteCache();
            } catch (Throwable ex) {
                logger.error("Rebuild cache failed", ex);
            }
        }, rebuildInterval, rebuildInterval, rebuildIntervalTimeUnit);
        scheduledExecutorService.scheduleWithFixedDelay(this::scanNewAppNamespaces, scanInterval,
                scanInterval, scanIntervalTimeUnit);
    }
    private void scanNewAppNamespaces() {
        try {
            this.loadNewAppNamespaces();
        } catch (Throwable ex) {
            logger.error("Load new app namespaces failed", ex);
        }
    }

    //for those new app namespaces
    private void loadNewAppNamespaces() {
        boolean hasMore = true;
        while (hasMore && !Thread.currentThread().isInterrupted()) {
            //current batch is 500
            List<AppNamespace> appNamespaces = appNamespaceRepository
                    .findFirst500ByIdGreaterThanOrderByIdAsc(maxIdScanned);
            if (CollectionUtils.isEmpty(appNamespaces)) {
                break;
            }
            mergeAppNamespaces(appNamespaces);
            int scanned = appNamespaces.size();
            maxIdScanned = appNamespaces.get(scanned - 1).getId();
            hasMore = scanned == 500;
            logger.info("Loaded {} new app namespaces with startId {}", scanned, maxIdScanned);
        }
    }

    private void mergeAppNamespaces(List<AppNamespace> appNamespaces) {
        for (AppNamespace appNamespace : appNamespaces) {
            appNamespaceCache.put(assembleAppNamespaceKey(appNamespace), appNamespace);
            appNamespaceIdCache.put(appNamespace.getId(), appNamespace);
            if (appNamespace.isPublic()) {
                publicAppNamespaceCache.put(appNamespace.getName(), appNamespace);
            }
        }
    }

    //for those updated or deleted app namespaces
    private void updateAndDeleteCache() {
        List<Long> ids = Lists.newArrayList(appNamespaceIdCache.keySet());
        if (CollectionUtils.isEmpty(ids)) {
            return;
        }
        List<List<Long>> partitionIds = Lists.partition(ids, 500);
        for (List<Long> toRebuild : partitionIds) {
            Iterable<AppNamespace> appNamespaces = appNamespaceRepository.findAll(toRebuild);

            if (appNamespaces == null) {
                continue;
            }

            //handle updated
            Set<Long> foundIds = handleUpdatedAppNamespaces(appNamespaces);

            //handle deleted
            handleDeletedAppNamespaces(Sets.difference(Sets.newHashSet(toRebuild), foundIds));
        }
    }

    //for those updated app namespaces
    private Set<Long> handleUpdatedAppNamespaces(Iterable<AppNamespace> appNamespaces) {
        Set<Long> foundIds = Sets.newHashSet();
        for (AppNamespace appNamespace : appNamespaces) {
            foundIds.add(appNamespace.getId());
            AppNamespace thatInCache = appNamespaceIdCache.get(appNamespace.getId());
            if (thatInCache != null && appNamespace.getDataChangeLastModifiedTime().after(thatInCache
                    .getDataChangeLastModifiedTime())) {
                appNamespaceIdCache.put(appNamespace.getId(), appNamespace);
                String oldKey = assembleAppNamespaceKey(thatInCache);
                String newKey = assembleAppNamespaceKey(appNamespace);
                appNamespaceCache.put(newKey, appNamespace);

                //in case appId or namespaceName changes
                if (!newKey.equals(oldKey)) {
                    appNamespaceCache.remove(oldKey);
                }

                if (appNamespace.isPublic()) {
                    publicAppNamespaceCache.put(appNamespace.getName(), appNamespace);

                    //in case namespaceName changes
                    if (!appNamespace.getName().equals(thatInCache.getName()) && thatInCache.isPublic()) {
                        publicAppNamespaceCache.remove(thatInCache.getName());
                    }
                } else if (thatInCache.isPublic()) {
                    //just in case isPublic changes
                    publicAppNamespaceCache.remove(thatInCache.getName());
                }
                logger.info("Found AppNamespace changes, old: {}, new: {}", thatInCache, appNamespace);
            }
        }
        return foundIds;
    }

    //for those deleted app namespaces
    private void handleDeletedAppNamespaces(Set<Long> deletedIds) {
        if (CollectionUtils.isEmpty(deletedIds)) {
            return;
        }
        for (Long deletedId : deletedIds) {
            AppNamespace deleted = appNamespaceIdCache.remove(deletedId);
            if (deleted == null) {
                continue;
            }
            appNamespaceCache.remove(assembleAppNamespaceKey(deleted));
            if (deleted.isPublic()) {
                publicAppNamespaceCache.remove(deleted.getName());
            }
            logger.info("Found AppNamespace deleted, {}", deleted);
        }
    }

    private String assembleAppNamespaceKey(AppNamespace appNamespace) {
        return STRING_JOINER.join(appNamespace.getAppId(), appNamespace.getName());
    }

    private void populateDataBaseInterval() {
        scanInterval = bizConfig.appNamespaceCacheScanInterval();
        scanIntervalTimeUnit = bizConfig.appNamespaceCacheScanIntervalTimeUnit();
        rebuildInterval = bizConfig.appNamespaceCacheRebuildInterval();
        rebuildIntervalTimeUnit = bizConfig.appNamespaceCacheRebuildIntervalTimeUnit();
    }
}
