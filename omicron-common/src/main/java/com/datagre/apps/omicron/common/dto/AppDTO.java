package com.datagre.apps.omicron.common.dto;

import lombok.Data;

@Data
public class AppDTO extends BaseDTO{

  private long id;

  private String name;

  private String appId;

  private String orgId;

  private String orgName;

  private String ownerName;

  private String ownerEmail;
}
