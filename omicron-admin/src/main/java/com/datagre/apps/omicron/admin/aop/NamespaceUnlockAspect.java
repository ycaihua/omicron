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
package com.datagre.apps.omicron.admin.aop;

import com.datagre.apps.omicron.biz.config.BizConfig;
import com.datagre.apps.omicron.biz.entity.Item;
import com.datagre.apps.omicron.biz.entity.Namespace;
import com.datagre.apps.omicron.biz.entity.Release;
import com.datagre.apps.omicron.biz.service.ItemService;
import com.datagre.apps.omicron.biz.service.NamespaceLockService;
import com.datagre.apps.omicron.biz.service.NamespaceService;
import com.datagre.apps.omicron.biz.service.ReleaseService;
import com.datagre.apps.omicron.common.constants.GsonType;
import com.datagre.apps.omicron.common.dto.ItemChangeSets;
import com.datagre.apps.omicron.common.dto.ItemDTO;
import com.datagre.apps.omicron.common.exception.BadRequestException;
import com.datagre.apps.omicron.core.utils.StringUtils;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import lombok.extern.apachecommons.CommonsLog;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Created by ycaihua on 2017/3/31.
 * https://github.com/ycaihua/omicron
 */
@Aspect
@Component
@CommonsLog
public class NamespaceUnlockAspect {
    private Gson gson=new Gson();
    @Autowired
    private NamespaceLockService namespaceLockService;
    @Autowired
    private NamespaceService namespaceService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private ReleaseService releaseService;
    @Autowired
    private BizConfig bizConfig;
    //create item
    @After("@annotation(PreAcquireNamespaceLock)&&args(appId, clusterName, namespaceName, item, ..)")
    public void requireLockAdvice(String appId, String clusterName, String namespaceName,
                                  ItemDTO item){
        tryUnlock(namespaceService.findOne(appId, clusterName, namespaceName));
    }
    //update item
    @After("@annotation(PreAcquireNamespaceLock) && args(appId, clusterName, namespaceName, itemId, item, ..)")
    public void requireLockAdvice(String appId, String clusterName, String namespaceName, long itemId,
                                  ItemDTO item) {
        tryUnlock(namespaceService.findOne(appId, clusterName, namespaceName));
    }

    //update by change set
    @After("@annotation(PreAcquireNamespaceLock) && args(appId, clusterName, namespaceName, changeSet, ..)")
    public void requireLockAdvice(String appId, String clusterName, String namespaceName,
                                  ItemChangeSets changeSet) {
        tryUnlock(namespaceService.findOne(appId, clusterName, namespaceName));
    }

    //delete item
    @After("@annotation(PreAcquireNamespaceLock) && args(itemId, operator, ..)")
    public void requireLockAdvice(long itemId, String operator) {
        Item item = itemService.findOne(itemId);
        if (item == null) {
            throw new BadRequestException("item not exist.");
        }
        tryUnlock(namespaceService.findOne(item.getNamespaceId()));
    }

    private void tryUnlock(Namespace namespace) {
        if (bizConfig.isNamespaceLockSwitchOff()) {
            return;
        }

        if (!isModified(namespace)) {
            namespaceLockService.unlock(namespace.getId());
        }

    }

    boolean isModified(Namespace namespace) {
        Release release = releaseService.findLatestActiveRelease(namespace);
        List<Item> items = itemService.findItems(namespace.getId());

        if (release == null) {
            return hasNormalItems(items);
        }

        Map<String, String> releasedConfiguration = gson.fromJson(release.getConfigurations(), GsonType.CONFIG);
        Map<String, String> configurationFromItems = generateConfigurationFromItems(namespace, items);

        MapDifference<String, String> difference = Maps.difference(releasedConfiguration, configurationFromItems);

        return !difference.areEqual();

    }

    private boolean hasNormalItems(List<Item> items) {
        for (Item item : items) {
            if (!StringUtils.isEmpty(item.getKey())) {
                return true;
            }
        }

        return false;
    }

    private Map<String, String> generateConfigurationFromItems(Namespace namespace, List<Item> namespaceItems) {

        Map<String, String> configurationFromItems = Maps.newHashMap();

        Namespace parentNamespace = namespaceService.findParentNamespace(namespace);
        //parent namespace
        if (parentNamespace == null) {
            generateMapFromItems(namespaceItems, configurationFromItems);
        } else {//child namespace
            Release parentRelease = releaseService.findLatestActiveRelease(parentNamespace);
            if (parentRelease != null) {
                configurationFromItems = gson.fromJson(parentRelease.getConfigurations(), GsonType.CONFIG);
            }
            generateMapFromItems(namespaceItems, configurationFromItems);
        }

        return configurationFromItems;
    }

    private Map<String, String> generateMapFromItems(List<Item> items, Map<String, String> configurationFromItems) {
        for (Item item : items) {
            String key = item.getKey();
            if (StringUtils.isBlank(key)) {
                continue;
            }
            configurationFromItems.put(key, item.getValue());
        }

        return configurationFromItems;
    }
}
