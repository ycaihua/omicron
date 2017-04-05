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
package com.datagre.apps.omicron.admin.controller;

import com.datagre.apps.omicron.admin.aop.PreAcquireNamespaceLock;
import com.datagre.apps.omicron.biz.entity.Commit;
import com.datagre.apps.omicron.biz.entity.Item;
import com.datagre.apps.omicron.biz.entity.Namespace;
import com.datagre.apps.omicron.biz.service.CommitService;
import com.datagre.apps.omicron.biz.service.ItemService;
import com.datagre.apps.omicron.biz.service.NamespaceService;
import com.datagre.apps.omicron.biz.utils.ConfigChangeContentBuilder;
import com.datagre.apps.omicron.common.dto.ItemDTO;
import com.datagre.apps.omicron.common.exception.BadRequestException;
import com.datagre.apps.omicron.common.exception.NotFoundException;
import com.datagre.apps.omicron.common.utils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created by ycaihua on 2017/4/5.
 * https://github.com/ycaihua/omicron
 */
@RestController
public class ItemController {
    @Autowired
    private ItemService itemService;
    @Autowired
    private NamespaceService namespaceService;
    @Autowired
    private CommitService commitService;

    @PreAcquireNamespaceLock
    @RequestMapping(path = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items", method = RequestMethod.POST)
    public ItemDTO create(@PathVariable("appId") String appId,
                          @PathVariable("clusterName") String clusterName,
                          @PathVariable("namespaceName") String namespaceName, @RequestBody ItemDTO dto) {
        Item entity = BeanUtils.transfrom(Item.class, dto);
        ConfigChangeContentBuilder builder = new ConfigChangeContentBuilder();
        Item managedEntity = itemService.findOne(appId, clusterName, namespaceName, entity.getKey());
        if (managedEntity != null) {
            throw new BadRequestException("item already exist");
        } else {
            entity = itemService.save(entity);
            builder.createItem(entity);
        }
        dto = BeanUtils.transfrom(ItemDTO.class, entity);
        Commit commit = new Commit();
        commit.setAppId(appId);
        commit.setClusterName(clusterName);
        commit.setNamespaceName(namespaceName);
        commit.setChangeSets(builder.build());
        commit.setDataChangeCreatedBy(dto.getDataChangeLastModifiedBy());
        commit.setDataChangeLastModifiedBy(dto.getDataChangeLastModifiedBy());
        commitService.save(commit);
        return dto;
    }

    @PreAcquireNamespaceLock
    @RequestMapping(path = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/{itemId}", method = RequestMethod.PUT)
    public ItemDTO update(@PathVariable("appId") String appId,
                          @PathVariable("clusterName") String clusterName,
                          @PathVariable("namespaceName") String namespaceName,
                          @PathVariable("itemId") long itemId,
                          @RequestBody ItemDTO itemDTO) {

        Item entity = BeanUtils.transfrom(Item.class, itemDTO);
        ConfigChangeContentBuilder builder = new ConfigChangeContentBuilder();
        Item managedEntity = itemService.findOne(itemId);
        if (managedEntity == null) {
            throw new BadRequestException("item not exist");
        }
        Item beforeUpdateItem = BeanUtils.transfrom(Item.class, managedEntity);
        //protect. only value,comment,lastModifiedBy can be modified
        managedEntity.setValue(entity.getValue());
        managedEntity.setComment(entity.getComment());
        managedEntity.setDataChangeLastModifiedBy(entity.getDataChangeLastModifiedBy());
        entity = itemService.update(managedEntity);
        builder.updateItem(beforeUpdateItem, entity);
        itemDTO = BeanUtils.transfrom(ItemDTO.class, entity);
        if (builder.hasContent()) {
            Commit commit = new Commit();
            commit.setAppId(appId);
            commit.setClusterName(clusterName);
            commit.setNamespaceName(namespaceName);
            commit.setChangeSets(builder.build());
            commit.setDataChangeCreatedBy(itemDTO.getDataChangeLastModifiedBy());
            commit.setDataChangeLastModifiedBy(itemDTO.getDataChangeLastModifiedBy());
            commitService.save(commit);
        }

        return itemDTO;
    }

    @PreAcquireNamespaceLock
    @RequestMapping(path = "/items/{itemId}", method = RequestMethod.DELETE)
    public void delete(@PathVariable("itemId") long itemId, @RequestParam String operator) {
        Item entity = itemService.findOne(itemId);
        if (entity == null) {
            throw new NotFoundException("item not found for itemId " + itemId);
        }
        itemService.delete(entity.getId(), operator);

        Namespace namespace = namespaceService.findOne(entity.getNamespaceId());

        Commit commit = new Commit();
        commit.setAppId(namespace.getAppId());
        commit.setClusterName(namespace.getClusterName());
        commit.setNamespaceName(namespace.getNamespaceName());
        commit.setChangeSets(new ConfigChangeContentBuilder().deleteItem(entity).build());
        commit.setDataChangeCreatedBy(operator);
        commit.setDataChangeLastModifiedBy(operator);
        commitService.save(commit);
    }

    @RequestMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items", method = RequestMethod.GET)
    public List<ItemDTO> findItems(@PathVariable("appId") String appId,
                                   @PathVariable("clusterName") String clusterName,
                                   @PathVariable("namespaceName") String namespaceName) {
        return BeanUtils.batchTransform(ItemDTO.class, itemService.findItems(appId, clusterName, namespaceName));
    }

    @RequestMapping(value = "/items/{itemId}", method = RequestMethod.GET)
    public ItemDTO get(@PathVariable("itemId") long itemId) {
        Item item = itemService.findOne(itemId);
        if (item == null) {
            throw new NotFoundException("item not found for itemId " + itemId);
        }
        return BeanUtils.transfrom(ItemDTO.class, item);
    }

    @RequestMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/{key:.+}", method = RequestMethod.GET)
    public ItemDTO get(@PathVariable("appId") String appId,
                       @PathVariable("clusterName") String clusterName,
                       @PathVariable("namespaceName") String namespaceName, @PathVariable("key") String key) {
        Item item = itemService.findOne(appId, clusterName, namespaceName, key);
        if (item == null) {
            throw new NotFoundException(
                    String.format("item not found for %s %s %s %s", appId, clusterName, namespaceName, key));
        }
        return BeanUtils.transfrom(ItemDTO.class, item);
    }

}
