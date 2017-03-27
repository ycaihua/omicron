package com.datagre.apps.omicron.biz.entity;

import com.datagre.apps.omicron.common.entity.BaseEntity;
import lombok.Data;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
/**
 * Created by zengxiaobo on 2017/3/27.
 */
@Data
@Entity
@Table(name = "GrayReleaseRule")
@SQLDelete(sql = "Update GrayReleaseRule set isDeleted = 1 where id = ?")
@Where(clause = "isDeleted = 0")
public class GrayReleaseRule extends BaseEntity {

  @Column(name = "appId", nullable = false)
  private String appId;

  @Column(name = "ClusterName", nullable = false)
  private String clusterName;

  @Column(name = "NamespaceName", nullable = false)
  private String namespaceName;

  @Column(name = "BranchName", nullable = false)
  private String branchName;

  @Column(name = "Rules")
  private String rules;

  @Column(name = "releaseId", nullable = false)
  private Long releaseId;

  @Column(name = "BranchStatus", nullable = false)
  private int branchStatus;

}
