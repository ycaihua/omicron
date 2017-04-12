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
package com.datagre.apps.omicron.client.spring.util;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import java.util.Objects;

/**
 * Created by ycaihua on 2017/4/11.
 * https://github.com/ycaihua/omicron
 */
public class BeanRegistrationUtil {
    public static boolean registerBeanDefinitionIfNotExists(BeanDefinitionRegistry registry,String beanName,
                                                            Class<?> beanClass){
        if (registry.containsBeanDefinition(beanName)){
            return false;
        }
        String[] candidates = registry.getBeanDefinitionNames();
        for (String candidate:candidates){
            BeanDefinition beanDefinition = registry.getBeanDefinition(candidate);
            if (Objects.equals(beanDefinition.getBeanClassName(),beanClass.getName())){
                return false;
            }
        }
        BeanDefinition annotationProcessor = BeanDefinitionBuilder.genericBeanDefinition(beanClass).getBeanDefinition();
        registry.registerBeanDefinition(beanName,annotationProcessor);
        return true;
    }
}
