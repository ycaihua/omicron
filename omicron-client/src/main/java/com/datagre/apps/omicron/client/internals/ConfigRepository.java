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

import java.util.Properties;

/**
 * Created by ycaihua on 2017/4/7.
 * https://github.com/ycaihua/omicron
 */
public interface ConfigRepository {
    /**
     * Get the config from this repository.
     * @return config
     */
    public Properties getConfig();

    /**
     * Set the fallback repo for this repository.
     * @param upstreamConfigRepository the upstream repo
     */
    public void setUpstreamRepository(ConfigRepository upstreamConfigRepository);

    /**
     * Add change listener.
     * @param listener the listener to observe the changes
     */
    public void addChangeListener(RepositoryChangeListener listener);

    /**
     * Remove change listener.
     * @param listener the listener to remove
     */
    public void removeChangeListener(RepositoryChangeListener listener);
}
