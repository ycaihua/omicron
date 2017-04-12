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

import com.google.common.collect.Maps;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;
import org.unidal.lookup.annotation.Named;

import java.util.Map;

/**
 * Created by ycaihua on 2017/4/10.
 * https://github.com/ycaihua/omicron
 */
@Named(type = ConfigRegistry.class)
public class DefaultConfigRegistry implements ConfigRegistry,LogEnabled {
    private Map<String,ConfigFactory> instances=Maps.newConcurrentMap();
    private Logger logger;
    @Override
    public void register(String namespace, ConfigFactory factory) {
        if (!instances.containsKey(namespace)){
            instances.put(namespace,factory);
        }

    }

    @Override
    public ConfigFactory getFactory(String namespace) {
        return instances.get(namespace);
    }

    @Override
    public void enableLogging(Logger logger) {
        logger = logger;
    }
}
