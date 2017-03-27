package com.datagre.apps.omicron.core.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Properties;
/**
 * Created by zengxiaobo on 2017/3/24.
 */
public class ResourceUtils {

  private static final Logger logger = LoggerFactory.getLogger(ResourceUtils.class);

  @SuppressWarnings("unchecked")
  public static Properties readConfigFile(String configPath, Properties defaults) {
    InputStream in = ClassLoaderUtil.getLoader().getResourceAsStream(configPath);
    logger.debug("Reading config from resource {}", configPath);
    Properties props = new Properties();
    try {
      if (in == null) {
        // load outside resource under current user path
        Path path = new File(System.getProperty("user.dir") + configPath).toPath();
        if (Files.isReadable(path)) {
          in = new FileInputStream(path.toFile());
          logger.debug("Reading config from file {} ", path);
        } else {
          logger.warn("Could not find available config file");
        }
      }
      if (defaults != null) {
        props.putAll(defaults);
      }

      if (in != null) {
        props.load(in);
        in.close();
      }
    } catch (Exception ex) {
      logger.warn("Reading config failed: {}", ex.getMessage());
    } finally {
      if (in != null) {
        try {
          in.close();
        } catch (IOException ex) {
          logger.warn("Close config failed: {}", ex.getMessage());
        }
      }
    }
    StringBuilder sb = new StringBuilder();
    for (Enumeration<String> e = (Enumeration<String>) props.propertyNames(); e
        .hasMoreElements();) {
      String key = e.nextElement();
      String val = (String) props.getProperty(key);
      sb.append(key).append('=').append(val).append('\n');
    }
    if (sb.length() > 0) {
      logger.debug("Reading properties: \n" + sb.toString());
    } else {
      logger.warn("No available properties");
    }
    return props;
  }
}