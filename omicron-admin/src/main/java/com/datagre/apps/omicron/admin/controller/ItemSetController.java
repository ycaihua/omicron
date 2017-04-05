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
import com.datagre.apps.omicron.biz.service.ItemSetService;
import com.datagre.apps.omicron.common.dto.ItemChangeSets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Created by ycaihua on 2017/4/5.
 * https://github.com/ycaihua/omicron
 */
@RestController
public class ItemSetController {
    @Autowired
    private ItemSetService itemSetService;
    @PreAcquireNamespaceLock
    @RequestMapping(path = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/itemset", method = RequestMethod.POST)
    public ResponseEntity<Void> create(@PathVariable String appId, @PathVariable String clusterName,
                                       @PathVariable String namespaceName, @RequestBody ItemChangeSets changeSet) {
        itemSetService.updateSet(appId, clusterName, namespaceName, changeSet);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}
