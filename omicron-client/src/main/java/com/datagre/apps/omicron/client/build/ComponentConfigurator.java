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
package com.datagre.apps.omicron.client.build;

import com.datagre.apps.omicron.client.internals.ConfigServiceLocator;
import com.datagre.apps.omicron.client.internals.DefaultConfigManager;
import com.datagre.apps.omicron.client.internals.RemoteConfigLongPollService;
import com.datagre.apps.omicron.client.spi.DefaultConfigFactory;
import com.datagre.apps.omicron.client.spi.DefaultConfigFactoryManager;
import com.datagre.apps.omicron.client.spi.DefaultConfigRegistry;
import com.datagre.apps.omicron.client.util.ConfigUtil;
import com.datagre.apps.omicron.client.util.http.HttpUtil;
import org.unidal.lookup.configuration.AbstractResourceConfigurator;
import org.unidal.lookup.configuration.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ycaihua on 2017/4/6.
 * https://github.com/ycaihua/omicron
 */
public class ComponentConfigurator extends AbstractResourceConfigurator {
    public static void main(String[] args){
        generatePlexusComponentsXmlFile(new ComponentConfigurator());
    }
    @Override
    public List<Component> defineComponents() {
        List<Component> all = new ArrayList<>();
        all.add(A(DefaultConfigManager.class));
        all.add(A(DefaultConfigFactory.class));
        all.add(A(DefaultConfigRegistry.class));
        all.add(A(DefaultConfigFactoryManager.class));
        all.add(A(ConfigUtil.class));
        all.add(A(HttpUtil.class));
        all.add(A(ConfigServiceLocator.class));
        all.add(A(RemoteConfigLongPollService.class));
        return all;
    }
}
