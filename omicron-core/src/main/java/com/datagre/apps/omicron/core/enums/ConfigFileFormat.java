package com.datagre.apps.omicron.core.enums;


import com.datagre.apps.omicron.core.utils.StringUtils;

/**
 * Created by zengxiaobo on 2017/3/24.
 */
public enum ConfigFileFormat {
  Properties("properties"), XML("xml"), JSON("json"), YML("yml"), YAML("yaml");

  private String value;

  ConfigFileFormat(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static ConfigFileFormat fromString(String value){
    if (StringUtils.isEmpty(value)){
      throw new IllegalArgumentException("value can not be empty");
    }
    switch (value){
      case "properties":
        return Properties;
      case "xml":
        return XML;
      case "json":
        return JSON;
      case "yml":
        return YML;
      case "yaml":
        return YAML;
    }
    throw new IllegalArgumentException(value + " can not map enum");
  }
}
