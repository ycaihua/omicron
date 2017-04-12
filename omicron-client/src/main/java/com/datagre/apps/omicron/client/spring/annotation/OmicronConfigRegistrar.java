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
package com.datagre.apps.omicron.client.spring.annotation;

import com.datagre.apps.omicron.client.spring.config.PropertySourcesProcessor;
import com.datagre.apps.omicron.client.spring.util.BeanRegistrationUtil;
import com.google.common.collect.Lists;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Created by ycaihua on 2017/4/11.
 * https://github.com/ycaihua/omicron
 */
public class OmicronConfigRegistrar implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(importingClassMetadata.getAnnotationAttributes(EnableOmicronConfig.class.getName()));
        String[] ns = attributes.getStringArray("value");
        int order = attributes.getNumber("order");
        PropertySourcesProcessor.addNamespaces(Lists.newArrayList(ns),order);
        BeanRegistrationUtil.registerBeanDefinitionIfNotExists(registry, PropertySourcesPlaceholderConfigurer.class.getName(),PropertySourcesPlaceholderConfigurer.class);
        BeanRegistrationUtil.registerBeanDefinitionIfNotExists(registry,PropertySourcesProcessor.class.getName(),PropertySourcesProcessor.class);
        BeanRegistrationUtil.registerBeanDefinitionIfNotExists(registry,OmicronAnnotationProcessor.class.getName(),OmicronAnnotationProcessor.class);
    }
}
