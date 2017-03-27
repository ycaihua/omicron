package com.datagre.apps.omicron.biz.entity;

import com.datagre.apps.omicron.common.entity.BaseEntity;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;

/**
 * Created by zengxiaobo on 2017/3/27.
 */
@Data
@Entity
@Table(name = "Release")
@SQLDelete(sql = "Update Release set isDeleted = 1 where id = ?")
@Where(clause = "isDeleted = 0")
public class Release extends BaseEntity {
  @Column(name = "ReleaseKey", nullable = false)
  private String releaseKey;

  @Column(name = "Name", nullable = false)
  private String name;

  @Column(name = "AppId", nullable = false)
  private String appId;

  @Column(name = "ClusterName", nullable = false)
  private String clusterName;

  @Column(name = "NamespaceName", nullable = false)
  private String namespaceName;

  @Column(name = "Configurations", nullable = false)
  @Lob
  private String configurations;

  @Column(name = "Comment", nullable = false)
  private String comment;

  @Column(name = "IsAbandoned", columnDefinition = "Bit default '0'")
  private boolean isAbandoned;

  public String toString() {
    return toStringHelper().add("name", name).add("appId", appId).add("clusterName", clusterName)
        .add("namespaceName", namespaceName).add("configurations", configurations)
        .add("comment", comment).add("isAbandoned", isAbandoned).toString();
  }
}
