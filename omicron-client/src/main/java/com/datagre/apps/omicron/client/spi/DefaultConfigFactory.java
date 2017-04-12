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
package com.datagre.apps.omicron.client.spi;

import com.datagre.apps.omicron.client.Config;
import com.datagre.apps.omicron.client.ConfigFile;
import com.datagre.apps.omicron.client.internals.ConfigRepository;
import com.datagre.apps.omicron.client.internals.DefaultConfig;
import com.datagre.apps.omicron.client.internals.LocalFileConfigRepository;
import com.datagre.apps.omicron.client.internals.RemoteConfigRepository;
import com.datagre.apps.omicron.client.util.ConfigUtil;
import com.datagre.apps.omicron.core.enums.ConfigFileFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unidal.lookup.annotation.Inject;
import org.unidal.lookup.annotation.Named;

/**
 * Created by ycaihua on 2017/4/6.
 * https://github.com/ycaihua/omicron
 */
@Named(type = ConfigFactory.class)
public class DefaultConfigFactory implements ConfigFactory {
    private static final Logger logger = LoggerFactory.getLogger(DefaultConfigFactory.class);
    @Inject
    private ConfigUtil configUtil;
    @Override
    public Config create(String namespace) {
        DefaultConfig defaultConfig = new DefaultConfig(namespace,createLocalConfigRepository(namespace));
        return defaultConfig;
    }

    private LocalFileConfigRepository createLocalConfigRepository(String namespace) {
        if (configUtil.isInLocalMode()) {
            logger.warn(
                    "==== Apollo is in local mode! Won't pull configs from remote server for namespace {} ! ====",
                    namespace);
            return new LocalFileConfigRepository(namespace);
        }
        return new LocalFileConfigRepository(namespace, createRemoteConfigRepository(namespace));
    }

    private RemoteConfigRepository createRemoteConfigRepository(String namespace) {
        return new RemoteConfigRepository(namespace);
    }

    @Override
    public ConfigFile createConfigFile(String namespace, ConfigFileFormat configFileFormat) {
        ConfigRepository configRepository=createLocalConfigRepository(namespace);
        switch (configFileFormat){
            case Properties:
        }
        return null;
    }
}
