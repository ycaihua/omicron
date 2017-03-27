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
@Table(name = "Instance")
public class Instance {
  @Id
  @GeneratedValue
  @Column(name = "Id")
  private long id;

  @Column(name = "AppId", nullable = false)
  private String appId;

  @Column(name = "ClusterName", nullable = false)
  private String clusterName;

  @Column(name = "DataCenter", nullable = false)
  private String dataCenter;

  @Column(name = "Ip", nullable = false)
  private String ip;

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

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("id", id)
        .add("appId", appId)
        .add("clusterName", clusterName)
        .add("dataCenter", dataCenter)
        .add("ip", ip)
        .add("dataChangeCreatedTime", dataChangeCreatedTime)
        .add("dataChangeLastModifiedTime", dataChangeLastModifiedTime)
        .toString();
  }
}
