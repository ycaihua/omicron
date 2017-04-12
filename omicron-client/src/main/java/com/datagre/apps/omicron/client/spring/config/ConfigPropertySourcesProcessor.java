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

import com.datagre.apps.omicron.client.spring.annotation.OmicronAnnotationProcessor;
import com.datagre.apps.omicron.client.spring.util.BeanRegistrationUtil;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

/**
 * Created by ycaihua on 2017/4/12.
 * https://github.com/ycaihua/omicron
 */
public class ConfigPropertySourcesProcessor extends PropertySourcesProcessor implements
        BeanDefinitionRegistryPostProcessor {
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        BeanRegistrationUtil.registerBeanDefinitionIfNotExists(registry, PropertySourcesPlaceholderConfigurer.class.getName(),
                PropertySourcesPlaceholderConfigurer.class);
        BeanRegistrationUtil.registerBeanDefinitionIfNotExists(registry, OmicronAnnotationProcessor.class.getName(),OmicronAnnotationProcessor.class);
    }
}
