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

import com.datagre.apps.omicron.biz.entity.Commit;
import com.datagre.apps.omicron.biz.service.CommitService;
import com.datagre.apps.omicron.common.dto.CommitDTO;
import com.datagre.apps.omicron.common.utils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by ycaihua on 2017/4/5.
 * https://github.com/ycaihua/omicron
 */
@RestController
public class CommitController {
    @Autowired
    private CommitService commitService;

    @RequestMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/commit", method = RequestMethod.GET)
    public List<CommitDTO> find(@PathVariable String appId, @PathVariable String clusterName,
                                @PathVariable String namespaceName, Pageable pageable){

        List<Commit> commits = commitService.find(appId, clusterName, namespaceName, pageable);
        return BeanUtils.batchTransform(CommitDTO.class, commits);
    }
}
