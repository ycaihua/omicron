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

import com.datagre.apps.omicron.biz.config.BizConfig;
import com.datagre.apps.omicron.biz.entity.Namespace;
import com.datagre.apps.omicron.biz.entity.NamespaceLock;
import com.datagre.apps.omicron.biz.service.NamespaceLockService;
import com.datagre.apps.omicron.biz.service.NamespaceService;
import com.datagre.apps.omicron.common.dto.NamespaceLockDTO;
import com.datagre.apps.omicron.common.exception.BadRequestException;
import com.datagre.apps.omicron.common.utils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by ycaihua on 2017/4/1.
 * https://github.com/ycaihua/omicron
 */
@RestController
public class NamespaceLockController {
    @Autowired
    private NamespaceLockService namespaceLockService;
    @Autowired
    private NamespaceService namespaceService;
    @Autowired
    private BizConfig bizConfig;
    @RequestMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/lock", method = RequestMethod.GET)
    public NamespaceLockDTO getNamespaceLockOwner(@PathVariable String appId, @PathVariable String clusterName,
                                                  @PathVariable String namespaceName) {
        Namespace namespace = namespaceService.findOne(appId, clusterName, namespaceName);
        if (namespace == null) {
            throw new BadRequestException("namespace not exist.");
        }

        if (bizConfig.isNamespaceLockSwitchOff()) {
            return null;
        }

        NamespaceLock lock = namespaceLockService.findLock(namespace.getId());

        if (lock == null) {
            return null;
        }

        return BeanUtils.transfrom(NamespaceLockDTO.class, lock);
    }
}
