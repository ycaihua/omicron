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

import com.datagre.apps.omicron.biz.entity.Namespace;
import com.datagre.apps.omicron.biz.service.NamespaceService;
import com.datagre.apps.omicron.common.dto.NamespaceDTO;
import com.datagre.apps.omicron.common.exception.BadRequestException;
import com.datagre.apps.omicron.common.exception.NotFoundException;
import com.datagre.apps.omicron.common.utils.BeanUtils;
import com.datagre.apps.omicron.common.utils.InputValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Created by ycaihua on 2017/4/1.
 * https://github.com/ycaihua/omicron
 */
@RestController
public class NamespaceController {
    @Autowired
    private NamespaceService namespaceService;
    @RequestMapping(path = "/apps/{appId}/clusters/{clusterName}/namespaces", method = RequestMethod.POST)
    public NamespaceDTO create(@PathVariable("appId") String appId,
                               @PathVariable("clusterName") String clusterName, @RequestBody NamespaceDTO dto) {
        if (!InputValidator.isValidClusterNamespace(dto.getNamespaceName())) {
            throw new BadRequestException(String.format("Namespace格式错误: %s", InputValidator.INVALID_CLUSTER_NAMESPACE_MESSAGE));
        }
        Namespace entity = BeanUtils.transfrom(Namespace.class, dto);
        Namespace managedEntity = namespaceService.findOne(appId, clusterName, entity.getNamespaceName());
        if (managedEntity != null) {
            throw new BadRequestException("namespace already exist.");
        }

        entity = namespaceService.save(entity);

        dto = BeanUtils.transfrom(NamespaceDTO.class, entity);
        return dto;
    }

    @RequestMapping(path = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName:.+}", method = RequestMethod.DELETE)
    public void delete(@PathVariable("appId") String appId,
                       @PathVariable("clusterName") String clusterName,
                       @PathVariable("namespaceName") String namespaceName, @RequestParam String operator) {
        Namespace entity = namespaceService.findOne(appId, clusterName, namespaceName);
        if (entity == null) throw new NotFoundException(
                String.format("namespace not found for %s %s %s", appId, clusterName, namespaceName));

        namespaceService.deleteNamespace(entity, operator);
    }

    @RequestMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces", method = RequestMethod.GET)
    public List<NamespaceDTO> find(@PathVariable("appId") String appId,
                                   @PathVariable("clusterName") String clusterName) {
        List<Namespace> groups = namespaceService.findNamespaces(appId, clusterName);
        return BeanUtils.batchTransform(NamespaceDTO.class, groups);
    }

    @RequestMapping(value = "/namespaces/{namespaceId}", method = RequestMethod.GET)
    public NamespaceDTO get(@PathVariable("namespaceId") Long namespaceId) {
        Namespace namespace = namespaceService.findOne(namespaceId);
        if (namespace == null)
            throw new NotFoundException(String.format("namespace not found for %s", namespaceId));
        return BeanUtils.transfrom(NamespaceDTO.class, namespace);
    }

    @RequestMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName:.+}", method = RequestMethod.GET)
    public NamespaceDTO get(@PathVariable("appId") String appId,
                            @PathVariable("clusterName") String clusterName,
                            @PathVariable("namespaceName") String namespaceName) {
        Namespace namespace = namespaceService.findOne(appId, clusterName, namespaceName);
        if (namespace == null) throw new NotFoundException(
                String.format("namespace not found for %s %s %s", appId, clusterName, namespaceName));
        return BeanUtils.transfrom(NamespaceDTO.class, namespace);
    }

    @RequestMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/associated-public-namespace",
            method = RequestMethod.GET)
    public NamespaceDTO findPublicNamespaceForAssociatedNamespace(@PathVariable String appId,
                                                                  @PathVariable String clusterName,
                                                                  @PathVariable String namespaceName) {
        Namespace namespace = namespaceService.findPublicNamespaceForAssociatedNamespace(clusterName, namespaceName);

        if (namespace == null) {
            throw new NotFoundException(String.format("public namespace not found. namespace:%s", namespaceName));
        }

        return BeanUtils.transfrom(NamespaceDTO.class, namespace);
    }

    /**
     * cluster -> cluster has not published namespaces?
     */
    @RequestMapping(value = "/apps/{appId}/namespaces/publish_info", method = RequestMethod.GET)
    public Map<String, Boolean> namespacePublishInfo(@PathVariable String appId) {
        return namespaceService.namespacePublishInfo(appId);
    }
}
