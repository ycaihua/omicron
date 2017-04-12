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
package com.datagre.apps.omicron.client.internals;

import com.datagre.apps.omicron.client.Config;
import com.datagre.apps.omicron.client.ConfigChangeListener;
import com.datagre.apps.omicron.client.enums.PropertyChangeType;
import com.datagre.apps.omicron.client.exceptions.OmicronConfigException;
import com.datagre.apps.omicron.client.model.ConfigChange;
import com.datagre.apps.omicron.client.model.ConfigChangeEvent;
import com.datagre.apps.omicron.client.util.ConfigUtil;
import com.datagre.apps.omicron.client.util.function.Functions;
import com.datagre.apps.omicron.client.util.parser.Parsers;
import com.datagre.apps.omicron.core.utils.OmicronThreadFactory;
import com.google.common.base.Function;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unidal.lookup.ContainerLoader;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
/**
 * Created by ycaihua on 2017/4/7.
 * https://github.com/ycaihua/omicron
 */
public abstract class AbstractConfig implements Config {
    private static final Logger logger= LoggerFactory.getLogger(AbstractConfig.class);
    private static ExecutorService executorService;
    private List<ConfigChangeListener> listeners= Lists.newCopyOnWriteArrayList();
    private ConfigUtil configUtil;
    private volatile Cache<String,Integer> integerCache;
    private volatile Cache<String, Long> longCache;
    private volatile Cache<String, Short> shortCache;
    private volatile Cache<String, Float> floatCache;
    private volatile Cache<String, Double> doubleCache;
    private volatile Cache<String, Byte> byteCache;
    private volatile Cache<String, Boolean> booleanCache;
    private volatile Cache<String, Date> dateCache;
    private volatile Cache<String, Long> durationCache;
    private Map<String,Cache<String,String[]>> arrayCache;
    private List<Cache> allCaches;
    private AtomicLong configVersion;
    static {
        executorService= Executors.newCachedThreadPool(OmicronThreadFactory.create("Config",true));
    }

    public AbstractConfig() {
        try {
            configUtil= ContainerLoader.getDefaultContainer().lookup(ConfigUtil.class);
            configVersion = new AtomicLong();
            arrayCache = Maps.newConcurrentMap();
            allCaches = Lists.newArrayList();
        }catch (ComponentLookupException ex){
            throw new OmicronConfigException("Unable to load component!", ex);
        }
    }

    @Override
    public Integer getIntProperty(String key, Integer defaultValue) {
        try {
            if (integerCache == null) {
                synchronized (this) {
                    if (integerCache == null) {
                        integerCache = newCache();
                    }
                }
            }

            return getValueFromCache(key,Functions.TO_INT_FUNCTION, integerCache, defaultValue);
        } catch (Throwable ex) {
        }
        return defaultValue;
    }

    @Override
    public Long getLongProperty(String key, Long defaultValue) {
        try {
            if (longCache == null) {
                synchronized (this) {
                    if (longCache == null) {
                        longCache = newCache();
                    }
                }
            }

            return getValueFromCache(key, Functions.TO_LONG_FUNCTION, longCache, defaultValue);
        } catch (Throwable ex) {
        }
        return defaultValue;
    }

    @Override
    public Short getShortProperty(String key, Short defaultValue) {
        try {
            if (shortCache == null) {
                synchronized (this) {
                    if (shortCache == null) {
                        shortCache = newCache();
                    }
                }
            }

            return getValueFromCache(key, Functions.TO_SHORT_FUNCTION, shortCache, defaultValue);
        } catch (Throwable ex) {
        }
        return defaultValue;
    }

    @Override
    public Float getFloatProperty(String key, Float defaultValue) {
        try {
            if (floatCache == null) {
                synchronized (this) {
                    if (floatCache == null) {
                        floatCache = newCache();
                    }
                }
            }

            return getValueFromCache(key, Functions.TO_FLOAT_FUNCTION, floatCache, defaultValue);
        } catch (Throwable ex) {
        }
        return defaultValue;
    }

    @Override
    public Double getDoubleProperty(String key, Double defaultValue) {
        try {
            if (doubleCache == null) {
                synchronized (this) {
                    if (doubleCache == null) {
                        doubleCache = newCache();
                    }
                }
            }

            return getValueFromCache(key, Functions.TO_DOUBLE_FUNCTION, doubleCache, defaultValue);
        } catch (Throwable ex) {
        }
        return defaultValue;
    }

    @Override
    public Byte getByteProperty(String key, Byte defaultValue) {
        try {
            if (byteCache == null) {
                synchronized (this) {
                    if (byteCache == null) {
                        byteCache = newCache();
                    }
                }
            }

            return getValueFromCache(key, Functions.TO_BYTE_FUNCTION, byteCache, defaultValue);
        } catch (Throwable ex) {
        }
        return defaultValue;
    }

    @Override
    public Boolean getBooleanProperty(String key, Boolean defaultValue) {
        try {
            if (booleanCache == null) {
                synchronized (this) {
                    if (booleanCache == null) {
                        booleanCache = newCache();
                    }
                }
            }

            return getValueFromCache(key, Functions.TO_BOOLEAN_FUNCTION, booleanCache, defaultValue);
        } catch (Throwable ex) {
        }
        return defaultValue;
    }

    @Override
    public String[] getArrayProperty(String key, final String delimiter, String[] defaultValue) {
        try {
            if (!arrayCache.containsKey(delimiter)) {
                synchronized (this) {
                    if (!arrayCache.containsKey(delimiter)) {
                        arrayCache.put(delimiter, this.<String[]>newCache());
                    }
                }
            }

            Cache<String, String[]> cache = arrayCache.get(delimiter);
            String[] result = cache.getIfPresent(key);

            if (result != null) {
                return result;
            }

            return getValueAndStoreToCache(key, new Function<String, String[]>() {
                @Override
                public String[] apply(String input) {
                    return input.split(delimiter);
                }
            }, cache, defaultValue);
        } catch (Throwable ex) {
        }
        return defaultValue;
    }

    @Override
    public <T extends Enum<T>> T getEnumProperty(String key, Class<T> enumType, T defaultValue) {
        try {
            String value = getProperty(key, null);

            if (value != null) {
                return Enum.valueOf(enumType, value);
            }
        } catch (Throwable ex) {
        }

        return defaultValue;
    }

    @Override
    public Date getDateProperty(String key, Date defaultValue) {
        try {
            if (dateCache == null) {
                synchronized (this) {
                    if (dateCache == null) {
                        dateCache = newCache();
                    }
                }
            }

            return getValueFromCache(key, Functions.TO_DATE_FUNCTION, dateCache, defaultValue);
        } catch (Throwable ex) {
        }

        return defaultValue;
    }

    @Override
    public Date getDateProperty(String key, String format, Date defaultValue) {
        try {
            String value = getProperty(key, null);

            if (value != null) {
                return Parsers.forDate().parse(value, format);
            }
        } catch (Throwable ex) {
        }

        return defaultValue;
    }

    @Override
    public Date getDateProperty(String key, String format, Locale locale, Date defaultValue) {
        try {
            String value = getProperty(key, null);

            if (value != null) {
                return Parsers.forDate().parse(value, format, locale);
            }
        } catch (Throwable ex) {
        }

        return defaultValue;
    }

    @Override
    public long getDurationProperty(String key, long defaultValue) {
        try {
            if (durationCache == null) {
                synchronized (this) {
                    if (durationCache == null) {
                        durationCache = newCache();
                    }
                }
            }

            return getValueFromCache(key, Functions.TO_DURATION_FUNCTION, durationCache, defaultValue);
        } catch (Throwable ex) {
        }

        return defaultValue;
    }

    private <T> T getValueFromCache(String key, Function<String, T> parser, Cache<String, T> cache, T defaultValue) {
        T result = cache.getIfPresent(key);

        if (result != null) {
            return result;
        }

        return getValueAndStoreToCache(key, parser, cache, defaultValue);
    }

    private <T> T getValueAndStoreToCache(String key, Function<String, T> parser, Cache<String, T> cache, T defaultValue) {
        long currentConfigVersion = configVersion.get();
        String value = getProperty(key, null);

        if (value != null) {
            T result = parser.apply(value);

            if (result != null) {
                synchronized (this) {
                    if (configVersion.get() == currentConfigVersion) {
                        cache.put(key, result);
                    }
                }
                return result;
            }
        }

        return defaultValue;
    }

    private <T> Cache<String, T> newCache() {
        Cache<String, T> cache = CacheBuilder.newBuilder()
                .maximumSize(configUtil.getMaxConfigCacheSize())
                .expireAfterAccess(configUtil.getConfigCacheExpireTime(), configUtil.getConfigCacheExpireTimeUnit())
                .build();
        allCaches.add(cache);
        return cache;
    }

    /**
     * Clear config cache
     */
    protected void clearConfigCache() {
        synchronized (this) {
            for (Cache c : allCaches) {
                if (c != null) {
                    c.invalidateAll();
                }
            }
            configVersion.incrementAndGet();
        }
    }

    protected void fireConfigChange(final ConfigChangeEvent changeEvent) {
        for (final ConfigChangeListener listener : listeners) {
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    String listenerName = listener.getClass().getName();
                    try {
                        listener.onChange(changeEvent);
                    } catch (Throwable ex) {
                        logger.error("Failed to invoke config change listener {}", listenerName, ex);
                    } finally {
                    }
                }
            });
        }
    }

    List<ConfigChange> calcPropertyChanges(String namespace, Properties previous,
                                           Properties current) {
        if (previous == null) {
            previous = new Properties();
        }

        if (current == null) {
            current = new Properties();
        }

        Set<String> previousKeys = previous.stringPropertyNames();
        Set<String> currentKeys = current.stringPropertyNames();

        Set<String> commonKeys = Sets.intersection(previousKeys, currentKeys);
        Set<String> newKeys = Sets.difference(currentKeys, commonKeys);
        Set<String> removedKeys = Sets.difference(previousKeys, commonKeys);

        List<ConfigChange> changes = Lists.newArrayList();

        for (String newKey : newKeys) {
            changes.add(new ConfigChange(namespace, newKey, null, current.getProperty(newKey),
                    PropertyChangeType.ADDED));
        }

        for (String removedKey : removedKeys) {
            changes.add(new ConfigChange(namespace, removedKey, previous.getProperty(removedKey), null,
                    PropertyChangeType.DELETED));
        }

        for (String commonKey : commonKeys) {
            String previousValue = previous.getProperty(commonKey);
            String currentValue = current.getProperty(commonKey);
            if (com.google.common.base.Objects.equal(previousValue, currentValue)) {
                continue;
            }
            changes.add(new ConfigChange(namespace, commonKey, previousValue, currentValue,
                    PropertyChangeType.MODIFIED));
        }

        return changes;
    }
    @Override
    public void addChangeListener(ConfigChangeListener listener) {
        if (!listeners.contains(listener)){
            listeners.add(listener);
        }
    }
}
