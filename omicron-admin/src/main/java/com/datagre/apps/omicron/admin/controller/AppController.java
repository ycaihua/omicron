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

import com.datagre.apps.omicron.biz.service.AdminService;
import com.datagre.apps.omicron.biz.service.AppService;
import com.datagre.apps.omicron.common.dto.AppDTO;
import com.datagre.apps.omicron.common.entity.App;
import com.datagre.apps.omicron.common.exception.BadRequestException;
import com.datagre.apps.omicron.common.exception.NotFoundException;
import com.datagre.apps.omicron.common.utils.BeanUtils;
import com.datagre.apps.omicron.common.utils.InputValidator;
import com.datagre.apps.omicron.core.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * Created by ycaihua on 2017/3/31.
 * https://github.com/ycaihua/omicron
 */
@RestController
public class AppController {
    @Autowired
    private AppService appService;
    @Autowired
    private AdminService adminService;
    @RequestMapping(path = "/apps",method = RequestMethod.POST)
    public AppDTO create(@RequestBody AppDTO dto){
        if (!InputValidator.isValidAppNamespace(dto.getAppId())){
            throw new BadRequestException(String.format("AppId格式错误: %s", InputValidator.INVALID_CLUSTER_NAMESPACE_MESSAGE));
        }
        App entity= BeanUtils.transfrom(App.class,dto);
        App manageEntity= appService.findOne(entity.getAppId());
        if (manageEntity!=null){
            throw new BadRequestException("app already exist.");
        }
        entity = adminService.createNewApp(entity);
        dto = BeanUtils.transfrom(AppDTO.class,entity);
        return dto;
    }

    @RequestMapping(value = "/apps/{appId}", method = RequestMethod.DELETE)
    public void delete(@PathVariable("appId") String appId, @RequestParam String operator) {
        App entity = appService.findOne(appId);
        if (entity == null) {
            throw new NotFoundException("app not found for appId " + appId);
        }
        appService.delete(entity.getId(), operator);
    }

    @RequestMapping(value = "/apps/{appId}", method = RequestMethod.PUT)
    public void update(@PathVariable String appId, @RequestBody App app) {
        if (!Objects.equals(appId, app.getAppId())) {
            throw new BadRequestException("The App Id of path variable and request body is different");
        }

        appService.update(app);
    }

    @RequestMapping(value = "/apps", method = RequestMethod.GET)
    public List<AppDTO> find(@RequestParam(value = "name", required = false) String name,
                             Pageable pageable) {
        List<App> app = null;
        if (StringUtils.isBlank(name)) {
            app = appService.findAll(pageable);
        } else {
            app = appService.findByName(name);
        }
        return BeanUtils.batchTransform(AppDTO.class, app);
    }

    @RequestMapping(value = "/apps/{appId}", method = RequestMethod.GET)
    public AppDTO get(@PathVariable("appId") String appId) {
        App app = appService.findOne(appId);
        if (app == null) {
            throw new NotFoundException("app not found for appId " + appId);
        }
        return BeanUtils.transfrom(AppDTO.class, app);
    }

    @RequestMapping(value = "/apps/{appId}/unique", method = RequestMethod.GET)
    public boolean isAppIdUnique(@PathVariable("appId") String appId) {
        return appService.isAppIdUnique(appId);
    }
}
