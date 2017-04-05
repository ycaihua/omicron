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
package com.datagre.apps.omicron.admin;

import com.datagre.apps.omicron.biz.OmicronBizConfig;
import com.datagre.apps.omicron.common.OmicronCommonConfig;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.system.ApplicationPidFileWriter;
import org.springframework.boot.system.EmbeddedServerPortFileWriter;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Created by ycaihua on 2017/3/31.
 * https://github.com/ycaihua/omicron
 */
@EnableAspectJAutoProxy
@EnableEurekaClient
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackageClasses = {OmicronCommonConfig.class,
        OmicronBizConfig.class,AdminServiceApplication.class})
@EnableTransactionManagement
public class AdminServiceApplication {
    public static void main(String[] args) {
        ConfigurableApplicationContext context = new SpringApplicationBuilder(AdminServiceApplication.class)
                .run(args);
        context.addApplicationListener(new ApplicationPidFileWriter());
        context.addApplicationListener(new EmbeddedServerPortFileWriter());
    }
}
