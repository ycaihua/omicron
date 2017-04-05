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
package com.datagre.apps.omicron.meta.service;

import com.datagre.apps.omicron.core.ServiceNameConsts;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ycaihua on 2017/4/5.
 * https://github.com/ycaihua/omicron
 */
@Service
@CommonsLog
public class DiscoveryService {
    @Autowired
    @Qualifier("eurekaClient")
    private EurekaClient eurekaClient;
    public List<InstanceInfo> getConfigServiceInstances(){
        Application application=eurekaClient.getApplication(ServiceNameConsts.OMICRON_CONFIGSERVICE);
        if (application==null){
            log.info("Apollo.EurekaDiscovery.NotFound:"+ServiceNameConsts.OMICRON_CONFIGSERVICE);
        }
        return application!=null?application.getInstances():new ArrayList<>();
    }
    public List<InstanceInfo> getMetaServiceInstances() {
        Application application = eurekaClient.getApplication(ServiceNameConsts.OMICRON_METASERVICE);
        if (application == null) {
            log.info("Apollo.EurekaDiscovery.NotFound"+ServiceNameConsts.OMICRON_METASERVICE);
        }
        return application != null ? application.getInstances() : new ArrayList<>();
    }

    public List<InstanceInfo> getAdminServiceInstances() {
        Application application = eurekaClient.getApplication(ServiceNameConsts.OMICRON_ADMINSERVICE);
        if (application == null) {
            log.info("Apollo.EurekaDiscovery.NotFound"+ServiceNameConsts.OMICRON_ADMINSERVICE);
        }
        return application != null ? application.getInstances() : new ArrayList<>();
    }
}
