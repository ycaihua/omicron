package com.datagre.apps.omicron.common.dto;

import lombok.Data;

@Data
public class ClusterDTO extends BaseDTO{

  private long id;

  private String name;

  private String appId;

  private long parentClusterId;
}
