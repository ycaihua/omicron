package com.datagre.apps.omicron.core.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Created by zengxiaobo on 2017/3/24.
 */
@Data
@NoArgsConstructor
@ToString
public class OmicronConfigNotification {
  private String namespaceName;
  private long notificationId;
  public OmicronConfigNotification(String namespaceName, long notificationId) {
    this.namespaceName = namespaceName;
    this.notificationId = notificationId;
  }
}
