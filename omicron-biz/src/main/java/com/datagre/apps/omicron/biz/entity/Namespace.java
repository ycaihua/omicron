package com.datagre.apps.omicron.biz.entity;

import com.datagre.apps.omicron.common.entity.BaseEntity;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
/**
 * Created by zengxiaobo on 2017/3/27.
 */
@Data
@NoArgsConstructor
@Entity
@Table(name = "Namespace")
@SQLDelete(sql = "Update Namespace set isDeleted = 1 where id = ?")
@Where(clause = "isDeleted = 0")
public class Namespace extends BaseEntity {

  @Column(name = "appId", nullable = false)
  private String appId;

  @Column(name = "ClusterName", nullable = false)
  private String clusterName;

  @Column(name = "NamespaceName", nullable = false)
  private String namespaceName;
    public Namespace(String appId, String clusterName, String namespaceName) {
        this.appId = appId;
        this.clusterName = clusterName;
        this.namespaceName = namespaceName;
    }
  public String toString() {
    return toStringHelper().add("appId", appId).add("clusterName", clusterName)
        .add("namespaceName", namespaceName).toString();
  }
}
