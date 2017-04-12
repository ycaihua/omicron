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
package com.datagre.apps.omicron.client.spring.config;

import com.datagre.apps.omicron.client.Config;
import com.datagre.apps.omicron.client.ConfigService;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Multimap;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;

import java.util.Collection;
import java.util.Iterator;

/**
 * Created by ycaihua on 2017/4/11.
 * https://github.com/ycaihua/omicron
 */
public class PropertySourcesProcessor implements BeanFactoryPostProcessor,EnvironmentAware,PriorityOrdered {
    private static final String PS_NAME= "OMICRON_PROPERTY_SOURCE_NAME";
    private static final Multimap<Integer,String> NAMESPACE_NAMES = HashMultimap.create();
    private ConfigurableEnvironment environment;
    public static boolean addNamespaces(Collection<String> namespaces, int order){
        return NAMESPACE_NAMES.putAll(order,namespaces);
    }
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        initializePropertySources();
    }

    private void initializePropertySources() {
        if (environment.getPropertySources().contains(PS_NAME)){
            return;
        }
        CompositePropertySource compositePropertySource = new CompositePropertySource(PS_NAME);
        ImmutableSortedSet<Integer> sortedSet = ImmutableSortedSet.copyOf(NAMESPACE_NAMES.keySet());
        sortedSet.stream().forEach(integer -> {
            NAMESPACE_NAMES.get(integer).stream().forEach(s -> {
                Config config = ConfigService.getConfig(s);
                compositePropertySource.addPropertySource(new ConfigPropertySource(s,config));
            });
        });
        environment.getPropertySources().addFirst(compositePropertySource);
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (ConfigurableEnvironment)environment;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
