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

import com.datagre.apps.omicron.core.enums.ConfigFileFormat;

/**
 * Created by ycaihua on 2017/4/6.
 * https://github.com/ycaihua/omicron
 */
public interface ConfigFile {
    /**
     * Get file content of the namespace
     * @return file content, {@code null} if there is no content
     */
    String getContent();

    /**
     * Whether the config file has any content
     * @return true if it has content, false otherwise.
     */
    boolean hasContent();

    /**
     * Get the namespace of this config file instance
     * @return the namespace
     */
    String getNamespace();

    /**
     * Get the file format of this config file instance
     * @return the config file format enum
     */
    ConfigFileFormat getConfigFileFormat();
}
