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
package com.datagre.apps.omicron.meta.controller;

import com.datagre.apps.omicron.core.dto.ServiceDTO;
import com.datagre.apps.omicron.meta.service.DiscoveryService;
import com.netflix.appinfo.InstanceInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by ycaihua on 2017/4/5.
 * https://github.com/ycaihua/omicron
 */
@RestController
@RequestMapping("/services")
public class ServiceController {
    @Autowired
    private DiscoveryService discoveryService;
    @RequestMapping("/meta")
    public List<ServiceDTO> getMetaService(){
        List<InstanceInfo> instances=discoveryService.getMetaServiceInstances();
        List<ServiceDTO> result = instances.stream().map(new Function<InstanceInfo, ServiceDTO>() {
            @Override
            public ServiceDTO apply(InstanceInfo instanceInfo) {
                ServiceDTO service=new ServiceDTO();
                service.setAppName(instanceInfo.getAppName());
                service.setInstanceId(instanceInfo.getInstanceId());
                service.setHomepageUrl(instanceInfo.getHomePageUrl());
                return service;
            }
        }).collect(Collectors.toList());
        return result;
    }
    @RequestMapping("/config")
    public List<ServiceDTO> getConfigService(
            @RequestParam(value = "appId", defaultValue = "") String appId,
            @RequestParam(value = "ip", required = false) String clientIp) {
        List<InstanceInfo> instances = discoveryService.getConfigServiceInstances();
        List<ServiceDTO> result = instances.stream().map(new Function<InstanceInfo, ServiceDTO>() {

            @Override
            public ServiceDTO apply(InstanceInfo instance) {
                ServiceDTO service = new ServiceDTO();
                service.setAppName(instance.getAppName());
                service.setInstanceId(instance.getInstanceId());
                service.setHomepageUrl(instance.getHomePageUrl());
                return service;
            }

        }).collect(Collectors.toList());
        return result;
    }

    @RequestMapping("/admin")
    public List<ServiceDTO> getAdminService() {
        List<InstanceInfo> instances = discoveryService.getAdminServiceInstances();
        List<ServiceDTO> result = instances.stream().map(new Function<InstanceInfo, ServiceDTO>() {

            @Override
            public ServiceDTO apply(InstanceInfo instance) {
                ServiceDTO service = new ServiceDTO();
                service.setAppName(instance.getAppName());
                service.setInstanceId(instance.getInstanceId());
                service.setHomepageUrl(instance.getHomePageUrl());
                return service;
            }

        }).collect(Collectors.toList());
        return result;
    }
}
