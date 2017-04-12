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

import com.datagre.apps.omicron.client.exceptions.OmicronConfigException;
import com.datagre.apps.omicron.client.util.ExceptionUtil;
import com.datagre.apps.omicron.core.enums.ConfigFileFormat;
import com.datagre.apps.omicron.core.utils.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by ycaihua on 2017/4/11.
 * https://github.com/ycaihua/omicron
 */
public class PropertiesConfigFile extends AbstractConfigFile {
    private static final Logger logger = LoggerFactory.getLogger(PropertiesConfigFile.class);
    private AtomicReference<String> m_contentCache;
    public PropertiesConfigFile(String namespace, ConfigRepository configRepository) {
        super(namespace, configRepository);
        m_contentCache = new AtomicReference<>();
    }
    @Override
    public String getContent() {
        if (m_contentCache.get()==null){
            m_contentCache.set(doGetContent());
        }
        return m_contentCache.get();
    }
    private String doGetContent() {
        if (!hasContent()){
            return null;
        }
        try {
            return PropertiesUtil.toString(configProperties.get());
        }catch (Throwable ex){
            OmicronConfigException exception = new OmicronConfigException(String.format("Parse properties file content failed for namespace: %s, cause: %s", namespace, ExceptionUtil.getDetailMessage(ex)));
            throw exception;
        }
    }

    @Override
    public boolean hasContent() {
        return configProperties.get()!=null&&!configProperties.get().isEmpty();
    }

    @Override
    public ConfigFileFormat getConfigFileFormat() {
        return ConfigFileFormat.Properties;
    }

    @Override
    public void onRepositoryChange(String namespace, Properties newProperties) {
        super.onRepositoryChange(namespace, newProperties);
        m_contentCache.set(null);
    }
}
