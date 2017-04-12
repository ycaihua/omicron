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
package com.datagre.apps.omicron.client;

import com.datagre.apps.omicron.client.exceptions.OmicronConfigException;
import com.datagre.apps.omicron.client.internals.ConfigManager;
import com.datagre.apps.omicron.client.spi.ConfigFactory;
import com.datagre.apps.omicron.client.spi.ConfigRegistry;
import com.datagre.apps.omicron.core.ConfigConsts;
import com.datagre.apps.omicron.core.enums.ConfigFileFormat;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.unidal.lookup.ContainerLoader;

/**
 * Created by ycaihua on 2017/4/6.
 * https://github.com/ycaihua/omicron
 */
public class ConfigService {
    private static final ConfigService s_instance = new ConfigService();
    private PlexusContainer container;
    private volatile ConfigManager m_configManager;
    private volatile ConfigRegistry m_configRegistry;
    private ConfigService(){
        container = ContainerLoader.getDefaultContainer();
    }
    private ConfigManager getManager(){
        if (m_configManager==null){
            synchronized (this){
                if (m_configManager==null){
                    try {
                        m_configManager = container.lookup(ConfigManager.class);
                    }catch (ComponentLookupException ex){
                        OmicronConfigException exception = new OmicronConfigException("Unable to load ConfigManager!", ex);
                        throw  exception;
                    }
                }
            }
        }
        return m_configManager;
    }
    private ConfigRegistry getRegistry(){
        if (m_configRegistry==null){
            synchronized (this){
                if (m_configRegistry==null){
                    try {
                        m_configRegistry=container.lookup(ConfigRegistry.class);
                    }catch (ComponentLookupException ex){
                        OmicronConfigException exception = new OmicronConfigException("Unable to load ConfigManager!", ex);
                        throw  exception;
                    }
                }
            }
        }
        return m_configRegistry;
    }
    /**
     * Get Application's config instance.
     *
     * @return config instance
     */
    public static Config getAppConfig() {
        return getConfig(ConfigConsts.NAMESPACE_APPLICATION);
    }

    /**
     * Get the config instance for the namespace.
     *
     * @param namespace the namespace of the config
     * @return config instance
     */
    public static Config getConfig(String namespace) {
        return s_instance.getManager().getConfig(namespace);
    }

    public static ConfigFile getConfigFile(String namespace, ConfigFileFormat configFileFormat) {
        return s_instance.getManager().getConfigFile(namespace, configFileFormat);
    }

    static void setConfig(Config config) {
        setConfig(ConfigConsts.NAMESPACE_APPLICATION, config);
    }

    /**
     * Manually set the config for the namespace specified, use with caution.
     *
     * @param namespace the namespace
     * @param config    the config instance
     */
    static void setConfig(String namespace, final Config config) {
        s_instance.getRegistry().register(namespace, new ConfigFactory() {
            @Override
            public Config create(String namespace) {
                return config;
            }

            @Override
            public ConfigFile createConfigFile(String namespace, ConfigFileFormat configFileFormat) {
                return null;
            }

        });
    }

    static void setConfigFactory(ConfigFactory factory) {
        setConfigFactory(ConfigConsts.NAMESPACE_APPLICATION, factory);
    }

    /**
     * Manually set the config factory for the namespace specified, use with caution.
     *
     * @param namespace the namespace
     * @param factory   the factory instance
     */
    static void setConfigFactory(String namespace, ConfigFactory factory) {
        s_instance.getRegistry().register(namespace, factory);
    }
}
