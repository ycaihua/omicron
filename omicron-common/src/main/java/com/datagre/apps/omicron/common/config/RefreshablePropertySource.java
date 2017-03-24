package com.datagre.apps.omicron.common.config;

import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * Created by zengxiaobo on 2017/3/24.
 */
public abstract class RefreshablePropertySource extends MapPropertySource{
    public RefreshablePropertySource(String name, Map<String, Object> source) {
        super(name, source);
    }

    @Override
    public Object getProperty(String name) {
        return this.source.get(name);
    }
    /**
     * refresh property
     */
    protected abstract void refresh();
}
