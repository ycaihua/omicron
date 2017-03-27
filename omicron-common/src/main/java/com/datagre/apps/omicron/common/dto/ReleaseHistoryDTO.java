package com.datagre.apps.omicron.common.dto;


import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
@Data
@NoArgsConstructor
public class ReleaseHistoryDTO extends BaseDTO{

  private long id;

  private String appId;

  private String clusterName;

  private String namespaceName;

  private String branchName;

  private long releaseId;

  private long previousReleaseId;

  private int operation;

  private Map<String, Object> operationContext;
}
