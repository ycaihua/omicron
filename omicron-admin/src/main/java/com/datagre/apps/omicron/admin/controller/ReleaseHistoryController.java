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

import com.datagre.apps.omicron.biz.entity.ReleaseHistory;
import com.datagre.apps.omicron.biz.service.ReleaseHistoryService;
import com.datagre.apps.omicron.common.dto.PageDTO;
import com.datagre.apps.omicron.common.dto.ReleaseHistoryDTO;
import com.datagre.apps.omicron.common.utils.BeanUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by ycaihua on 2017/4/5.
 * https://github.com/ycaihua/omicron
 */
@RestController
public class ReleaseHistoryController {
    private Gson gson = new Gson();
    private Type configurationTypeReference = new TypeToken<Map<String, Object>>() {
    }.getType();

    @Autowired
    private ReleaseHistoryService releaseHistoryService;

    @RequestMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/releases/histories",
            method = RequestMethod.GET)
    public PageDTO<ReleaseHistoryDTO> findReleaseHistoriesByNamespace(
            @PathVariable String appId, @PathVariable String clusterName,
            @PathVariable String namespaceName,
            Pageable pageable) {

        Page<ReleaseHistory> result = releaseHistoryService.findReleaseHistoriesByNamespace(appId, clusterName,
                namespaceName, pageable);
        return transform2PageDTO(result, pageable);
    }


    @RequestMapping(value = "/releases/histories/by_release_id_and_operation", method = RequestMethod.GET)
    public PageDTO<ReleaseHistoryDTO> findReleaseHistoryByReleaseIdAndOperation(
            @RequestParam("releaseId") long releaseId,
            @RequestParam("operation") int operation,
            Pageable pageable) {

        Page<ReleaseHistory> result = releaseHistoryService.findByReleaseIdAndOperation(releaseId, operation, pageable);

        return transform2PageDTO(result, pageable);
    }

    @RequestMapping(value = "/releases/histories/by_previous_release_id_and_operation", method = RequestMethod.GET)
    public PageDTO<ReleaseHistoryDTO> findReleaseHistoryByPreviousReleaseIdAndOperation(
            @RequestParam("previousReleaseId") long previousReleaseId,
            @RequestParam("operation") int operation,
            Pageable pageable) {

        Page<ReleaseHistory> result = releaseHistoryService.findByPreviousReleaseIdAndOperation(previousReleaseId, operation, pageable);

        return transform2PageDTO(result, pageable);

    }

    private PageDTO<ReleaseHistoryDTO> transform2PageDTO(Page<ReleaseHistory> releaseHistoriesPage, Pageable pageable){
        if (!releaseHistoriesPage.hasContent()) {
            return null;
        }

        List<ReleaseHistory> releaseHistories = releaseHistoriesPage.getContent();
        List<ReleaseHistoryDTO> releaseHistoryDTOs = new ArrayList<>(releaseHistories.size());
        for (ReleaseHistory releaseHistory : releaseHistories) {
            releaseHistoryDTOs.add(transformReleaseHistory2DTO(releaseHistory));
        }

        return new PageDTO<>(releaseHistoryDTOs, pageable, releaseHistoriesPage.getTotalElements());
    }

    private ReleaseHistoryDTO transformReleaseHistory2DTO(ReleaseHistory releaseHistory) {
        ReleaseHistoryDTO dto = new ReleaseHistoryDTO();
        BeanUtils.copyProperties(releaseHistory, dto, "operationContext");
        dto.setOperationContext(gson.fromJson(releaseHistory.getOperationContext(),
                configurationTypeReference));

        return dto;
    }
}
