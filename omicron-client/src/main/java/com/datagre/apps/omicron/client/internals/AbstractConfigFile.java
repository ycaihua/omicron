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

import com.datagre.apps.omicron.client.ConfigFile;
import com.datagre.apps.omicron.client.util.ExceptionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by ycaihua on 2017/4/7.
 * https://github.com/ycaihua/omicron
 */
public abstract class AbstractConfigFile implements ConfigFile,RepositoryChangeListener{
    private static final Logger logger= LoggerFactory.getLogger(AbstractConfig.class);
    protected ConfigRepository configRepository;
    protected String namespace;
    protected AtomicReference<Properties> configProperties;
    public AbstractConfigFile(String namespace, ConfigRepository configRepository) {
        configRepository = configRepository;
        namespace = namespace;
        configProperties = new AtomicReference<>();
        initialize();
    }

    protected void initialize(){
        try {
            configProperties.set(configRepository.getConfig());
        }catch (Throwable ex){
            logger.warn("Init Apollo Config File failed - namespace: {}, reason: {}.",
                    namespace, ExceptionUtil.getDetailMessage(ex));
        }finally {
            configRepository.addChangeListener(this);
        }
    }

    @Override
    public void onRepositoryChange(String namespace, Properties newProperties) {
        if (newProperties.equals(configProperties.get())){
            return;
        }
        Properties newConfigProperties = new Properties();
        newConfigProperties.putAll(newProperties);
        configProperties.set(newConfigProperties);
    }

    @Override
    public String getNamespace() {
        return namespace;
    }
}
