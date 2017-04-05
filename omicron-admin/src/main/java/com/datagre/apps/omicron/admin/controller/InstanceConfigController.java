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

import com.datagre.apps.omicron.biz.entity.Instance;
import com.datagre.apps.omicron.biz.entity.InstanceConfig;
import com.datagre.apps.omicron.biz.entity.Release;
import com.datagre.apps.omicron.biz.service.InstanceService;
import com.datagre.apps.omicron.biz.service.ReleaseService;
import com.datagre.apps.omicron.common.dto.InstanceConfigDTO;
import com.datagre.apps.omicron.common.dto.InstanceDTO;
import com.datagre.apps.omicron.common.dto.PageDTO;
import com.datagre.apps.omicron.common.dto.ReleaseDTO;
import com.datagre.apps.omicron.common.exception.NotFoundException;
import com.datagre.apps.omicron.common.utils.BeanUtils;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by ycaihua on 2017/4/5.
 * https://github.com/ycaihua/omicron
 */
@RestController
@RequestMapping("/instances")
public class InstanceConfigController {
    private static final Splitter RELEASES_SPLITTER = Splitter.on(",").omitEmptyStrings()
            .trimResults();
    @Autowired
    private ReleaseService releaseService;
    @Autowired
    private InstanceService instanceService;

    @RequestMapping(value = "/by-release", method = RequestMethod.GET)
    public PageDTO<InstanceDTO> getByRelease(@RequestParam("releaseId") long releaseId,
                                             Pageable pageable) {
        Release release = releaseService.findOne(releaseId);
        if (release == null) {
            throw new NotFoundException(String.format("release not found for %s", releaseId));
        }
        Page<InstanceConfig> instanceConfigsPage = instanceService.findActiveInstanceConfigsByReleaseKey
                (release.getReleaseKey(), pageable);

        List<InstanceDTO> instanceDTOs = Collections.emptyList();

        if (instanceConfigsPage.hasContent()) {
            Multimap<Long, InstanceConfig> instanceConfigMap = HashMultimap.create();
            Set<String> otherReleaseKeys = Sets.newHashSet();

            for (InstanceConfig instanceConfig : instanceConfigsPage.getContent()) {
                instanceConfigMap.put(instanceConfig.getInstanceId(), instanceConfig);
                otherReleaseKeys.add(instanceConfig.getReleaseKey());
            }

            Set<Long> instanceIds = instanceConfigMap.keySet();

            List<Instance> instances = instanceService.findInstancesByIds(instanceIds);

            if (!CollectionUtils.isEmpty(instances)) {
                instanceDTOs = BeanUtils.batchTransform(InstanceDTO.class, instances);
            }

            for (InstanceDTO instanceDTO : instanceDTOs) {
                Collection<InstanceConfig> configs = instanceConfigMap.get(instanceDTO.getId());
                List<InstanceConfigDTO> configDTOs = configs.stream().map(instanceConfig -> {
                    InstanceConfigDTO instanceConfigDTO = new InstanceConfigDTO();
                    //to save some space
                    instanceConfigDTO.setRelease(null);
                    instanceConfigDTO.setReleaseDeliveryTime(instanceConfig.getReleaseDeliveryTime());
                    instanceConfigDTO.setDataChangeLastModifiedTime(instanceConfig
                            .getDataChangeLastModifiedTime());
                    return instanceConfigDTO;
                }).collect(Collectors.toList());
                instanceDTO.setConfigs(configDTOs);
            }
        }

        return new PageDTO<>(instanceDTOs, pageable, instanceConfigsPage.getTotalElements());
    }

    @RequestMapping(value = "/by-namespace-and-releases-not-in", method = RequestMethod.GET)
    public List<InstanceDTO> getByReleasesNotIn(@RequestParam("appId") String appId,
                                                @RequestParam("clusterName") String clusterName,
                                                @RequestParam("namespaceName") String namespaceName,
                                                @RequestParam("releaseIds") String releaseIds) {
        Set<Long> releaseIdSet = RELEASES_SPLITTER.splitToList(releaseIds).stream().map(Long::parseLong)
                .collect(Collectors.toSet());

        List<Release> releases = releaseService.findByReleaseIds(releaseIdSet);

        if (CollectionUtils.isEmpty(releases)) {
            throw new NotFoundException(String.format("releases not found for %s", releaseIds));
        }

        Set<String> releaseKeys = releases.stream().map(Release::getReleaseKey).collect(Collectors
                .toSet());

        List<InstanceConfig> instanceConfigs = instanceService
                .findInstanceConfigsByNamespaceWithReleaseKeysNotIn(appId, clusterName, namespaceName,
                        releaseKeys);

        Multimap<Long, InstanceConfig> instanceConfigMap = HashMultimap.create();
        Set<String> otherReleaseKeys = Sets.newHashSet();

        for (InstanceConfig instanceConfig : instanceConfigs) {
            instanceConfigMap.put(instanceConfig.getInstanceId(), instanceConfig);
            otherReleaseKeys.add(instanceConfig.getReleaseKey());
        }

        List<Instance> instances = instanceService.findInstancesByIds(instanceConfigMap.keySet());

        if (CollectionUtils.isEmpty(instances)) {
            return Collections.emptyList();
        }

        List<InstanceDTO> instanceDTOs = BeanUtils.batchTransform(InstanceDTO.class, instances);

        List<Release> otherReleases = releaseService.findByReleaseKeys(otherReleaseKeys);
        Map<String, ReleaseDTO> releaseMap = Maps.newHashMap();

        for (Release release : otherReleases) {
            //unset configurations to save space
            release.setConfigurations(null);
            ReleaseDTO releaseDTO = BeanUtils.transfrom(ReleaseDTO.class, release);
            releaseMap.put(release.getReleaseKey(), releaseDTO);
        }

        for (InstanceDTO instanceDTO : instanceDTOs) {
            Collection<InstanceConfig> configs = instanceConfigMap.get(instanceDTO.getId());
            List<InstanceConfigDTO> configDTOs = configs.stream().map(instanceConfig -> {
                InstanceConfigDTO instanceConfigDTO = new InstanceConfigDTO();
                instanceConfigDTO.setRelease(releaseMap.get(instanceConfig.getReleaseKey()));
                instanceConfigDTO.setReleaseDeliveryTime(instanceConfig.getReleaseDeliveryTime());
                instanceConfigDTO.setDataChangeLastModifiedTime(instanceConfig
                        .getDataChangeLastModifiedTime());
                return instanceConfigDTO;
            }).collect(Collectors.toList());
            instanceDTO.setConfigs(configDTOs);
        }

        return instanceDTOs;
    }

    @RequestMapping(value = "/by-namespace", method = RequestMethod.GET)
    public PageDTO<InstanceDTO> getInstancesByNamespace(
            @RequestParam("appId") String appId, @RequestParam("clusterName") String clusterName,
            @RequestParam("namespaceName") String namespaceName,
            @RequestParam(value = "instanceAppId", required = false) String instanceAppId,
            Pageable pageable) {
        Page<Instance> instances;
        if (Strings.isNullOrEmpty(instanceAppId)) {
            instances = instanceService.findInstancesByNamespace(appId, clusterName,
                    namespaceName, pageable);
        } else {
            instances = instanceService.findInstancesByNamespaceAndInstanceAppId(instanceAppId, appId,
                    clusterName, namespaceName, pageable);
        }

        List<InstanceDTO> instanceDTOs = BeanUtils.batchTransform(InstanceDTO.class, instances.getContent());
        return new PageDTO<>(instanceDTOs, pageable, instances.getTotalElements());
    }

    @RequestMapping(value = "/by-namespace/count", method = RequestMethod.GET)
    public long getInstancesCountByNamespace(@RequestParam("appId") String appId,
                                             @RequestParam("clusterName") String clusterName,
                                             @RequestParam("namespaceName") String namespaceName) {
        Page<Instance> instances = instanceService.findInstancesByNamespace(appId, clusterName,
                namespaceName, new PageRequest(0, 1));
        return instances.getTotalElements();
    }
}
