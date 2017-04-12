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
import com.datagre.apps.omicron.core.enums.ConfigFileFormat;

/**
 * Created by ycaihua on 2017/4/7.
 * https://github.com/ycaihua/omicron
 */
public interface ConfigManager {
    /**
     * Get the config instance for the namespace specified.
     * @param namespace the namespace
     * @return the config instance for the namespace
     */
    public Config getConfig(String namespace);

    /**
     * Get the config file instance for the namespace specified.
     * @param namespace the namespace
     * @param configFileFormat the config file format
     * @return the config file instance for the namespace
     */
    public ConfigFile getConfigFile(String namespace, ConfigFileFormat configFileFormat);
}
