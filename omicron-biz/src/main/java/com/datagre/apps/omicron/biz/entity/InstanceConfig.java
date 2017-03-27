package com.datagre.apps.omicron.biz.entity;

import com.google.common.base.MoreObjects;
import lombok.Data;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by zengxiaobo on 2017/3/27.
 */
@Data
@Entity
@Table(name = "InstanceConfig")
public class InstanceConfig {
  @Id
  @GeneratedValue
  @Column(name = "Id")
  private long id;

  @Column(name = "InstanceId")
  private long instanceId;

  @Column(name = "ConfigAppId", nullable = false)
  private String configAppId;

  @Column(name = "ConfigClusterName", nullable = false)
  private String configClusterName;

  @Column(name = "ConfigNamespaceName", nullable = false)
  private String configNamespaceName;

  @Column(name = "ReleaseKey", nullable = false)
  private String releaseKey;

  @Column(name = "ReleaseDeliveryTime", nullable = false)
  private Date releaseDeliveryTime;

  @Column(name = "DataChange_CreatedTime", nullable = false)
  private Date dataChangeCreatedTime;

  @Column(name = "DataChange_LastTime")
  private Date dataChangeLastModifiedTime;

  @PrePersist
  protected void prePersist() {
    if (this.dataChangeCreatedTime == null) {
      dataChangeCreatedTime = new Date();
    }
    if (this.dataChangeLastModifiedTime == null) {
      dataChangeLastModifiedTime = dataChangeCreatedTime;
    }
  }

  @PreUpdate
  protected void preUpdate() {
    this.dataChangeLastModifiedTime = new Date();
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("id", id)
        .add("configAppId", configAppId)
        .add("configClusterName", configClusterName)
        .add("configNamespaceName", configNamespaceName)
        .add("releaseKey", releaseKey)
        .add("dataChangeCreatedTime", dataChangeCreatedTime)
        .add("dataChangeLastModifiedTime", dataChangeLastModifiedTime)
        .toString();
  }
}
