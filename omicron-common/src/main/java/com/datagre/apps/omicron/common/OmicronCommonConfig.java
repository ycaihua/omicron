package com.datagre.apps.omicron.common;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by zengxiaobo on 2017/3/24.
 */
@EnableAutoConfiguration
@Configuration
@ComponentScan(basePackageClasses = OmicronCommonConfig.class)
public class OmicronCommonConfig {
}
