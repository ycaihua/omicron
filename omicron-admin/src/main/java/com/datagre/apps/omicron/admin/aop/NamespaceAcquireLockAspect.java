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
import com.datagre.apps.omicron.biz.entity.NamespaceLock;
import com.datagre.apps.omicron.biz.service.ItemService;
import com.datagre.apps.omicron.biz.service.NamespaceLockService;
import com.datagre.apps.omicron.biz.service.NamespaceService;
import com.datagre.apps.omicron.common.dto.ItemChangeSets;
import com.datagre.apps.omicron.common.dto.ItemDTO;
import com.datagre.apps.omicron.common.exception.BadRequestException;
import com.datagre.apps.omicron.common.exception.ServiceException;
import lombok.extern.apachecommons.CommonsLog;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Created by ycaihua on 2017/3/28.
 * https://github.com/ycaihua/omicron
 */
@Aspect
@Component
@CommonsLog
public class NamespaceAcquireLockAspect {
    @Autowired
    private NamespaceLockService namespaceLockService;
    @Autowired
    private NamespaceService namespaceService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private BizConfig bizConfig;
    //create item
    @Before("@annotation(PreAcquireNamespaceLock)&&args(appId, clusterName, namespaceName, item, ..)")
    public void requireLockAdvice(String appId, String clusterName, String namespaceName, ItemDTO item){
        acquireLock(appId, clusterName, namespaceName, item.getDataChangeLastModifiedBy());
    }


    //update item
    @Before("@annotation(PreAcquireNamespaceLock) && args(appId, clusterName, namespaceName, itemId, item, ..)")
    public void requireLockAdvice(String appId, String clusterName, String namespaceName, long itemId,
                                  ItemDTO item) {
        acquireLock(appId, clusterName, namespaceName, item.getDataChangeLastModifiedBy());
    }

    //update by change set
    @Before("@annotation(PreAcquireNamespaceLock) && args(appId, clusterName, namespaceName, changeSet, ..)")
    public void requireLockAdvice(String appId, String clusterName, String namespaceName,
                                  ItemChangeSets changeSet) {
        acquireLock(appId, clusterName, namespaceName, changeSet.getDataChangeLastModifiedBy());
    }

    //delete item
    @Before("@annotation(PreAcquireNamespaceLock) && args(itemId, operator, ..)")
    public void requireLockAdvice(long itemId, String operator) {
        Item item = itemService.findOne(itemId);
        if (item == null){
            throw new BadRequestException("item not exist.");
        }
        acquireLock(item.getNamespaceId(), operator);
    }

    void acquireLock(String appId, String clusterName, String namespaceName,
                     String currentUser) {
        if (bizConfig.isNamespaceLockSwitchOff()) {
            return;
        }

        Namespace namespace = namespaceService.findOne(appId, clusterName, namespaceName);

        acquireLock(namespace, currentUser);
    }

    void acquireLock(long namespaceId, String currentUser) {
        if (bizConfig.isNamespaceLockSwitchOff()) {
            return;
        }

        Namespace namespace = namespaceService.findOne(namespaceId);

        acquireLock(namespace, currentUser);

    }

    private void acquireLock(Namespace namespace, String currentUser) {
        if (namespace == null) {
            throw new BadRequestException("namespace not exist.");
        }

        long namespaceId = namespace.getId();

        NamespaceLock namespaceLock = namespaceLockService.findLock(namespaceId);
        if (namespaceLock == null) {
            try {
                tryLock(namespaceId, currentUser);
                //lock success
            } catch (DataIntegrityViolationException e) {
                //lock fail
                namespaceLock = namespaceLockService.findLock(namespaceId);
                checkLock(namespace, namespaceLock, currentUser);
            } catch (Exception e) {
                log.error("try lock error", e);
                throw e;
            }
        } else {
            //check lock owner is current user
            checkLock(namespace, namespaceLock, currentUser);
        }
    }

    private void tryLock(long namespaceId, String user) {
        NamespaceLock lock = new NamespaceLock();
        lock.setNamespaceId(namespaceId);
        lock.setDataChangeCreatedBy(user);
        lock.setDataChangeLastModifiedBy(user);
        namespaceLockService.tryLock(lock);
    }

    private void checkLock(Namespace namespace, NamespaceLock namespaceLock,
                           String currentUser) {
        if (namespaceLock == null) {
            throw new ServiceException(
                    String.format("Check lock for %s failed, please retry.", namespace.getNamespaceName()));
        }

        String lockOwner = namespaceLock.getDataChangeCreatedBy();
        if (!lockOwner.equals(currentUser)) {
            throw new BadRequestException(
                    "namespace:" + namespace.getNamespaceName() + " is modified by " + lockOwner);
        }
    }
}
