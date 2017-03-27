package com.datagre.apps.omicron.common.dto;

import lombok.Data;

@Data
public class AppNamespaceDTO extends BaseDTO{
  private long id;
  private String name;
  private String appId;
  private String comment;
  private String format;
  private boolean isPublic = false;
}
