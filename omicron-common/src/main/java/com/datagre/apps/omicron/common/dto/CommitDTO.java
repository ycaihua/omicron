package com.datagre.apps.omicron.common.dto;

import lombok.Data;

@Data
public class CommitDTO extends BaseDTO{

  private String changeSets;

  private String appId;

  private String clusterName;

  private String namespaceName;

  private String comment;
}
