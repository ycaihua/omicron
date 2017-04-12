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

import com.datagre.apps.omicron.Omicron;
import com.datagre.apps.omicron.client.exceptions.OmicronConfigException;
import com.datagre.apps.omicron.client.util.ExceptionUtil;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Properties;

/**
 * Created by ycaihua on 2017/4/7.
 * https://github.com/ycaihua/omicron
 */
public abstract class AbstractConfigRepository implements ConfigRepository{
    private static final Logger logger = LoggerFactory.getLogger(AbstractConfigRepository.class);
    private List<RepositoryChangeListener> listeners = Lists.newCopyOnWriteArrayList();

    protected boolean trySync() {
        try {
            sync();
            return true;
        } catch (Throwable ex) {
            logger
                    .warn("Sync config failed, will retry. Repository {}, reason: {}", this.getClass(), ExceptionUtil
                            .getDetailMessage(ex));
        }
        return false;
    }

    protected abstract void sync();

    @Override
    public void addChangeListener(RepositoryChangeListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeChangeListener(RepositoryChangeListener listener) {
        listeners.remove(listener);
    }

    protected void fireRepositoryChange(String namespace, Properties newProperties) {
        for (RepositoryChangeListener listener : listeners) {
            try {
                listener.onRepositoryChange(namespace, newProperties);
            } catch (Throwable ex) {
                logger.error("Failed to invoke repository change listener {}", listener.getClass(), ex);
            }
        }
    }
}
