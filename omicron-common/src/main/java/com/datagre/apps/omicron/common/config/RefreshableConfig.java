package com.datagre.apps.omicron.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * Created by zengxiaobo on 2017/3/24.
 */
public abstract class RefreshableConfig {
    private static final Logger logger = LoggerFactory.getLogger(RefreshableConfig.class);
    private static final String LIST_SEPARATOR = ",";
    //TimeUnit: second
    private static final int CONFIG_REFRESH_INTERVAL = 60;

}
