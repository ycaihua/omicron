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

import com.datagre.apps.omicron.Omicron;
import com.datagre.apps.omicron.client.exceptions.OmicronConfigException;
import com.datagre.apps.omicron.client.util.ConfigUtil;
import com.datagre.apps.omicron.client.util.ExceptionUtil;
import com.datagre.apps.omicron.core.ConfigConsts;
import com.datagre.apps.omicron.core.utils.ClassLoaderUtil;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.unidal.lookup.ContainerLoader;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Created by ycaihua on 2017/4/10.
 * https://github.com/ycaihua/omicron
 */
public class LocalFileConfigRepository extends AbstractConfigRepository implements RepositoryChangeListener {
    private static final Logger logger = LoggerFactory.getLogger(LocalFileConfigRepository.class);
    private static final String CONFIG_DIR = "/config-cache";
    private final PlexusContainer container;
    private final String namespace;
    private File baseDir;
    private final ConfigUtil configUtil;
    private volatile Properties fileProperties;
    private volatile ConfigRepository upstream;

    public LocalFileConfigRepository(String namespace) {
        this(namespace,null);
    }
    public LocalFileConfigRepository(String ns, ConfigRepository upstream) {
        namespace = ns;
        container = ContainerLoader.getDefaultContainer();
        try {
            configUtil = container.lookup(ConfigUtil.class);
        }catch (ComponentLookupException ex){
            throw new OmicronConfigException("Unable to load component!", ex);
        }
        this.setLocalCacheDir(findLocalCacheDir(),false);
        this.setUpstreamRepository(upstream);
        this.trySync();
    }

    void setLocalCacheDir(File localCacheDir, boolean syncImmediately) {
        baseDir = baseDir;
        this.checkLocalConfigCacheDir(baseDir);
        if (syncImmediately) {
            this.trySync();
        }
    }

    File findLocalCacheDir() {
        try {
            String defaultCacheDir = configUtil.getDefaultLocalCacheDir();
            Path path = Paths.get(defaultCacheDir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            if (Files.exists(path) && Files.isWritable(path)) {
                return new File(defaultCacheDir, CONFIG_DIR);
            }
        } catch (Throwable ex) {
            //ignore
        }

        return new File(ClassLoaderUtil.getClassPath(), CONFIG_DIR);
    }

    @Override
    public Properties getConfig() {
        if (fileProperties==null){
            sync();
        }
        Properties result = new Properties();
        result.putAll(fileProperties);
        return result;
    }

    @Override
    public void setUpstreamRepository(ConfigRepository upstreamConfigRepository) {
        if (upstreamConfigRepository==null){
            return;
        }
        if (upstream!=null){
            upstream.removeChangeListener(this);
        }
        trySyncFromUpstream();
        upstreamConfigRepository.addChangeListener(this);
    }

    private boolean trySyncFromUpstream() {
        if (upstream == null) {
            return false;
        }
        try {
            Properties properties = upstream.getConfig();
            updateFileProperties(properties);
            return true;
        } catch (Throwable ex) {
            logger.warn("Sync config from upstream repository {} failed, reason: {}", upstream.getClass(),
                            ExceptionUtil.getDetailMessage(ex));
        }
        return false;
    }

    private synchronized void updateFileProperties(Properties newProperties) {
        if (newProperties.equals(fileProperties)) {
            return;
        }
        this.fileProperties = newProperties;
        persistLocalCacheFile(baseDir, namespace);
    }

    private void persistLocalCacheFile(File baseDir, String namespace) {
        if (baseDir == null) {
            return;
        }
        File file = assembleLocalCacheFile(baseDir, namespace);
        OutputStream out = null;
        try {
            out = new FileOutputStream(file);
            fileProperties.store(out, "Persisted by DefaultConfig");
        } catch (IOException ex) {
            OmicronConfigException exception = new OmicronConfigException(String.format("Persist local cache file %s failed", file.getAbsolutePath()), ex);
            logger.warn("Persist local cache file {} failed, reason: {}.", file.getAbsolutePath(), ExceptionUtil.getDetailMessage(ex));
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    //ignore
                }
            }
        }
    }

    private File assembleLocalCacheFile(File baseDir, String namespace) {
        String fileName = String.format("%s.properties", Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR).join(configUtil.getAppId(), configUtil.getCluster(), namespace));
        return new File(baseDir, fileName);
    }

    @Override
    public void onRepositoryChange(String namespace, Properties newProperties) {
        if (newProperties.equals(fileProperties)) {
            return;
        }
        Properties newFileProperties = new Properties();
        newFileProperties.putAll(newProperties);
        updateFileProperties(newFileProperties);
        this.fireRepositoryChange(namespace, newProperties);
    }

    @Override
    protected void sync() {
        //sync with upstream immediately
        boolean syncFromUpstreamResultSuccess = trySyncFromUpstream();

        if (syncFromUpstreamResultSuccess) {
            return;
        }
        Throwable exception = null;
        try {
            fileProperties = this.loadFromLocalCacheFile(baseDir, namespace);
        } catch (Throwable ex) {
            exception = ex;
            //ignore
        }
        if (fileProperties == null) {
            throw new OmicronConfigException(
                    "Load config from local config failed!", exception);
        }
    }

    private Properties loadFromLocalCacheFile(File baseDir, String namespace) {
        Preconditions.checkNotNull(baseDir, "Basedir cannot be null");
        File file = assembleLocalCacheFile(baseDir, namespace);
        Properties properties = null;

        if (file.isFile() && file.canRead()) {
            InputStream in = null;

            try {
                in = new FileInputStream(file);
                properties = new Properties();
                properties.load(in);
                logger.debug("Loading local config file {} successfully!", file.getAbsolutePath());
            } catch (IOException ex) {
                throw new OmicronConfigException(String.format("Loading config from local cache file %s failed", file.getAbsolutePath()), ex);
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ex) {
                    // ignore
                }
            }
        } else {
            throw new OmicronConfigException(String.format("Cannot read from local cache file %s", file.getAbsolutePath()));
        }

        return properties;
    }

    public void checkLocalConfigCacheDir(File baseDir){
        if (baseDir.exists()){
            return;
        }
        try {
            Files.createDirectory(baseDir.toPath());
        }catch (IOException ex){
            OmicronConfigException exception =
                    new OmicronConfigException(
                            String.format("Create local config directory %s failed", baseDir.getAbsolutePath()),
                            ex);
            logger.warn(
                    "Unable to create local config cache directory {}, reason: {}. Will not able to cache config file.",
                    baseDir.getAbsolutePath(), ExceptionUtil.getDetailMessage(ex));
        }
    }
}
