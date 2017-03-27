package com.datagre.apps.omicron.common.dto;

import lombok.Data;

@Data
public class NamespaceDTO extends BaseDTO{
  private long id;

  private String appId;
  
  private String clusterName;

  private String namespaceName;
}
