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
package com.datagre.apps.omicron.client.model;

import java.util.Map;
import java.util.Set;

/**
 * Created by ycaihua on 2017/4/6.
 * https://github.com/ycaihua/omicron
 */
public class ConfigChangeEvent {
    private final String m_namespace;
    private final Map<String, ConfigChange> m_changes;

    /**
     * Constructor.
     * @param namespace the namespace of this change
     * @param changes the actual changes
     */
    public ConfigChangeEvent(String namespace,
                             Map<String, ConfigChange> changes) {
        m_namespace = namespace;
        m_changes = changes;
    }

    /**
     * Get the keys changed.
     * @return the list of the keys
     */
    public Set<String> changedKeys() {
        return m_changes.keySet();
    }

    /**
     * Get a specific change instance for the key specified.
     * @param key the changed key
     * @return the change instance
     */
    public ConfigChange getChange(String key) {
        return m_changes.get(key);
    }

    /**
     * Check whether the specified key is changed
     * @param key the key
     * @return true if the key is changed, false otherwise.
     */
    public boolean isChanged(String key) {
        return m_changes.containsKey(key);
    }

    /**
     * Get the namespace of this change event.
     * @return the namespace
     */
    public String getNamespace() {
        return m_namespace;
    }
}
