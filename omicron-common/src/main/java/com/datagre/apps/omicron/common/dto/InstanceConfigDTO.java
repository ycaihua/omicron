package com.datagre.apps.omicron.common.dto;

import lombok.Data;

import java.util.Date;

@Data
public class InstanceConfigDTO {
  private ReleaseDTO release;
  private Date releaseDeliveryTime;
  private Date dataChangeLastModifiedTime;
}
