package com.datagre.apps.omicron.core.dto;

import lombok.Data;

@Data
public class ServiceDTO {

  private String appName;

  private String instanceId;

  private String homepageUrl;

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ServiceDTO{");
    sb.append("appName='").append(appName).append('\'');
    sb.append(", instanceId='").append(instanceId).append('\'');
    sb.append(", homepageUrl='").append(homepageUrl).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
