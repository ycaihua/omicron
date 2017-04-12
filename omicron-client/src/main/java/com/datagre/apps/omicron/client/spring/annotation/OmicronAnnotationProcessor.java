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

import com.datagre.apps.omicron.Omicron;
import com.datagre.apps.omicron.client.Config;
import com.datagre.apps.omicron.client.ConfigChangeListener;
import com.datagre.apps.omicron.client.ConfigService;
import com.datagre.apps.omicron.client.model.ConfigChangeEvent;
import com.google.common.base.Preconditions;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by ycaihua on 2017/4/11.
 * https://github.com/ycaihua/omicron
 */
public class OmicronAnnotationProcessor implements BeanPostProcessor,PriorityOrdered {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Class clazz =bean.getClass();
        processFields(bean, clazz.getDeclaredFields());
        processMethods(bean, clazz.getDeclaredMethods());
        return bean;
    }

    private void processMethods(Object bean, Method[] declaredMethods) {
        Arrays.stream(declaredMethods).forEach(method -> {
            OmicronConfigChangeListener annotation = AnnotationUtils.getAnnotation(method,OmicronConfigChangeListener.class);
            if (annotation==null){
                return;
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            Preconditions.checkArgument(parameterTypes.length == 1,
                    "Invalid number of parameters: %s for method: %s, should be 1", parameterTypes.length, method);
            Preconditions.checkArgument(ConfigChangeEvent.class.isAssignableFrom(parameterTypes[0]),
                    "Invalid parameter type: %s for method: %s, should be ConfigChangeEvent", parameterTypes[0], method);
            ReflectionUtils.makeAccessible(method);
            String[] nss = annotation.value();
            Arrays.stream(nss).forEach(ns -> {
                Config config = ConfigService.getConfig(ns);
                config.addChangeListener(changeEvent -> {
                    ReflectionUtils.invokeMethod(method,bean,changeEvent);
                });
            });
        });
    }

    private void processFields(Object bean, Field[] declaredFields) {
        Arrays.stream(declaredFields).forEach(field -> {
            OmicronConfig annotation = AnnotationUtils.getAnnotation(field,OmicronConfig.class);
            if (annotation==null){
                return;
            }
            Preconditions.checkArgument(Config.class.isAssignableFrom(field.getType()),"Invalid type: %s for field: %s, should be Config", field.getType(), field);
            String ns = annotation.value();
            Config config=ConfigService.getConfig(ns);
            ReflectionUtils.makeAccessible(field);
            ReflectionUtils.setField(field,bean,config);
        });
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
