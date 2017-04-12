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
package com.datagre.apps.omicron.client.internals;

import com.datagre.apps.omicron.client.Config;
import com.datagre.apps.omicron.client.ConfigFile;
import com.datagre.apps.omicron.client.spi.ConfigFactory;
import com.datagre.apps.omicron.client.spi.ConfigFactoryManager;
import com.datagre.apps.omicron.core.enums.ConfigFileFormat;
import com.google.common.collect.Maps;
import org.unidal.lookup.annotation.Inject;
import org.unidal.lookup.annotation.Named;

import java.util.Map;

/**
 * Created by ycaihua on 2017/4/10.
 * https://github.com/ycaihua/omicron
 */
@Named(type = ConfigManager.class)
public class DefaultConfigManager implements ConfigManager{
    @Inject
    private ConfigFactoryManager configFactoryManager;
    private Map<String,Config> configMap= Maps.newConcurrentMap();
    private Map<String,ConfigFile>  configFileMap = Maps.newConcurrentMap();

    @Override
    public Config getConfig(String namespace) {
        Config config= configMap.get(namespace);
        if (config==null){
            synchronized (this){
                config = configMap.get(namespace);
                if (config==null){
                    ConfigFactory configFactory=configFactoryManager.getFactory(namespace);
                    config = configFactory.create(namespace);
                    configMap.put(namespace,config);
                }
            }
        }
        return config;
    }

    @Override
    public ConfigFile getConfigFile(String namespace, ConfigFileFormat configFileFormat) {
        String namespaceFileName=String.format("%s.%s",namespace,configFileFormat.getValue());
        ConfigFile configFile= configFileMap.get(namespaceFileName);
        if (configFile==null){
            synchronized (this){
                configFile = configFileMap.get(namespaceFileName);
                if (configFile==null){
                    ConfigFactory configFactory=configFactoryManager.getFactory(namespaceFileName);
                    configFile= configFactory.createConfigFile(namespaceFileName,configFileFormat);
                    configFileMap.put(namespaceFileName,configFile);
                }
            }
        }
        return configFile;
    }
}
