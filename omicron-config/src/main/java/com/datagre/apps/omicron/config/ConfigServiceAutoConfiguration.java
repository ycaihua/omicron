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
package com.datagre.apps.omicron.config;

import com.datagre.apps.omicron.biz.grayReleaseRule.GrayReleaseRulesHolder;
import com.datagre.apps.omicron.biz.message.ReleaseMessageScanner;
import com.datagre.apps.omicron.config.controller.ConfigFileController;
import com.datagre.apps.omicron.config.controller.NotificationController;
import com.datagre.apps.omicron.config.controller.NotificationControllerV2;
import com.datagre.apps.omicron.config.service.ReleaseMessageCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Created by ycaihua on 2017/4/6.
 * https://github.com/ycaihua/omicron
 */
@Configuration
public class ConfigServiceAutoConfiguration {
    @Bean
    GrayReleaseRulesHolder grayReleaseRulesHolder(){
        return new GrayReleaseRulesHolder();
    }
    @Configuration
    static class MessageScannerConfiguration {
        @Autowired
        private NotificationController notificationController;
        @Autowired
        private ConfigFileController configFileController;
        @Autowired
        private NotificationControllerV2 notificationControllerV2;
        @Autowired
        private GrayReleaseRulesHolder grayReleaseRulesHolder;
        @Autowired
        private ReleaseMessageCacheService releaseMessageCacheService;

        @Bean
        public ReleaseMessageScanner releaseMessageScanner() {
            ReleaseMessageScanner releaseMessageScanner = new ReleaseMessageScanner();
            //0. handle release message cache
            releaseMessageScanner.addMessageListener(releaseMessageCacheService);
            //1. handle gray release rule
            releaseMessageScanner.addMessageListener(grayReleaseRulesHolder);
            //2. handle server cache
            releaseMessageScanner.addMessageListener(configFileController);
            //3. notify clients
            releaseMessageScanner.addMessageListener(notificationControllerV2);
            releaseMessageScanner.addMessageListener(notificationController);
            return releaseMessageScanner;
        }
}
