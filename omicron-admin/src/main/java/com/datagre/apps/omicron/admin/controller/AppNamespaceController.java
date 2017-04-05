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
import com.datagre.apps.omicron.biz.service.AppNamespaceService;
import com.datagre.apps.omicron.biz.service.NamespaceService;
import com.datagre.apps.omicron.common.dto.AppNamespaceDTO;
import com.datagre.apps.omicron.common.dto.NamespaceDTO;
import com.datagre.apps.omicron.common.entity.AppNamespace;
import com.datagre.apps.omicron.common.exception.BadRequestException;
import com.datagre.apps.omicron.common.utils.BeanUtils;
import com.datagre.apps.omicron.core.enums.ConfigFileFormat;
import com.datagre.apps.omicron.core.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created by ycaihua on 2017/4/1.
 * https://github.com/ycaihua/omicron
 */
@RestController
public class AppNamespaceController {
    @Autowired
    private AppNamespaceService appNamespaceService;
    @Autowired
    private NamespaceService namespaceService;
    @RequestMapping(value = "/apps/{appId}/appnamespaces", method = RequestMethod.POST)
    public AppNamespaceDTO create(@RequestBody AppNamespaceDTO appNamespace) {

        AppNamespace entity = BeanUtils.transfrom(AppNamespace.class, appNamespace);
        AppNamespace managedEntity = appNamespaceService.findOne(entity.getAppId(), entity.getName());

        if (managedEntity != null) {
            throw new BadRequestException("app namespaces already exist.");
        }

        if (StringUtils.isEmpty(entity.getFormat())){
            entity.setFormat(ConfigFileFormat.Properties.getValue());
        }

        entity = appNamespaceService.createAppNamespace(entity);

        return BeanUtils.transfrom(AppNamespaceDTO.class, entity);

    }

    @RequestMapping(value = "/appnamespaces/{publicNamespaceName}/namespaces", method = RequestMethod.GET)
    public List<NamespaceDTO> findPublicAppNamespaceAllNamespaces(@PathVariable String publicNamespaceName, Pageable pageable) {

        List<Namespace> namespaces = namespaceService.findPublicAppNamespaceAllNamespaces(publicNamespaceName, pageable);

        return BeanUtils.batchTransform(NamespaceDTO.class, namespaces);
    }

    @RequestMapping(value = "/appnamespaces/{publicNamespaceName}/associated-namespaces/count", method = RequestMethod.GET)
    public int countPublicAppNamespaceAssociatedNamespaces(@PathVariable String publicNamespaceName) {
        return namespaceService.countPublicAppNamespaceAssociatedNamespaces(publicNamespaceName);
    }
}
