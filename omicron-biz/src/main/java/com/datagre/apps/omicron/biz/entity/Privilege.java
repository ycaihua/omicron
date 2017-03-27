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
@Table(name = "Privilege")
@SQLDelete(sql = "Update Privilege set isDeleted = 1 where id = ?")
@Where(clause = "isDeleted = 0")
public class Privilege extends BaseEntity {

  @Column(name = "Name", nullable = false)
  private String name;

  @Column(name = "PrivilType", nullable = false)
  private String privilType;

  @Column(name = "NamespaceId")
  private long namespaceId;

  public String toString() {
    return toStringHelper().add("namespaceId", namespaceId).add("privilType", privilType)
        .add("name", name).toString();
  }
}
