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

import com.datagre.apps.omicron.biz.entity.Cluster;
import com.datagre.apps.omicron.biz.service.ClusterService;
import com.datagre.apps.omicron.common.dto.ClusterDTO;
import com.datagre.apps.omicron.common.exception.BadRequestException;
import com.datagre.apps.omicron.common.exception.NotFoundException;
import com.datagre.apps.omicron.common.utils.BeanUtils;
import com.datagre.apps.omicron.common.utils.InputValidator;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created by ycaihua on 2017/4/1.
 * https://github.com/ycaihua/omicron
 */
@RestController
@CommonsLog
public class ClusterController {
    @Autowired
    private ClusterService clusterService;
    @RequestMapping(path = "/apps/{appId}/clusters", method = RequestMethod.POST)
    public ClusterDTO create(@PathVariable("appId") String appId,
                             @RequestParam(value = "autoCreatePrivateNamespace", defaultValue = "true") boolean autoCreatePrivateNamespace,
                             @RequestBody ClusterDTO dto) {
        if (!InputValidator.isValidClusterNamespace(dto.getName())) {
            throw new BadRequestException(String.format("Cluster格式错误: %s", InputValidator.INVALID_CLUSTER_NAMESPACE_MESSAGE));
        }

        Cluster entity = BeanUtils.transfrom(Cluster.class, dto);
        Cluster managedEntity = clusterService.findOne(appId, entity.getName());
        if (managedEntity != null) {
            throw new BadRequestException("cluster already exist.");
        }

        if (autoCreatePrivateNamespace) {
            entity = clusterService.saveWithInstanceOfAppNamespaces(entity);
        } else {
            entity = clusterService.saveWithoutInstanceOfAppNamespaces(entity);
        }

        dto = BeanUtils.transfrom(ClusterDTO.class, entity);
        return dto;
    }

    @RequestMapping(path = "/apps/{appId}/clusters/{clusterName:.+}", method = RequestMethod.DELETE)
    public void delete(@PathVariable("appId") String appId,
                       @PathVariable("clusterName") String clusterName, @RequestParam String operator) {
        Cluster entity = clusterService.findOne(appId, clusterName);
        if (entity == null) {
            throw new NotFoundException("cluster not found for clusterName " + clusterName);
        }
        clusterService.delete(entity.getId(), operator);
    }

    @RequestMapping(value = "/apps/{appId}/clusters", method = RequestMethod.GET)
    public List<ClusterDTO> find(@PathVariable("appId") String appId) {
        List<Cluster> clusters = clusterService.findParentClusters(appId);
        return BeanUtils.batchTransform(ClusterDTO.class, clusters);
    }

    @RequestMapping(value = "/apps/{appId}/clusters/{clusterName:.+}", method = RequestMethod.GET)
    public ClusterDTO get(@PathVariable("appId") String appId,
                          @PathVariable("clusterName") String clusterName) {
        Cluster cluster = clusterService.findOne(appId, clusterName);
        if (cluster == null) {
            throw new NotFoundException("cluster not found for name " + clusterName);
        }
        return BeanUtils.transfrom(ClusterDTO.class, cluster);
    }

    @RequestMapping(value = "/apps/{appId}/cluster/{clusterName}/unique", method = RequestMethod.GET)
    public boolean isAppIdUnique(@PathVariable("appId") String appId,
                                 @PathVariable("clusterName") String clusterName) {
        return clusterService.isClusterNameUnique(appId, clusterName);
    }
}
