package com.datagre.apps.omicron.core.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Created by zengxiaobo on 2017/3/24.
 */
@Data
@NoArgsConstructor
public class OmicronConfig {

  private String appId;

  private String cluster;

  private String namespaceName;

  private Map<String, String> configurations;

  private String releaseKey;

  public OmicronConfig(String appId,
                      String cluster,
                      String namespaceName,
                      String releaseKey) {
    this.appId = appId;
    this.cluster = cluster;
    this.namespaceName = namespaceName;
    this.releaseKey = releaseKey;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("OmicronConfig{");
    sb.append("appId='").append(appId).append('\'');
    sb.append(", cluster='").append(cluster).append('\'');
    sb.append(", namespaceName='").append(namespaceName).append('\'');
    sb.append(", configurations=").append(configurations);
    sb.append(", releaseKey='").append(releaseKey).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
