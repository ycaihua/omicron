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
@Table(name = "ServerConfig")
@SQLDelete(sql = "Update ServerConfig set isDeleted = 1 where id = ?")
@Where(clause = "isDeleted = 0")
public class ServerConfig extends BaseEntity {
  @Column(name = "Key", nullable = false)
  private String key;

  @Column(name = "Cluster", nullable = false)
  private String cluster;

  @Column(name = "Value", nullable = false)
  private String value;

  @Column(name = "Comment", nullable = false)
  private String comment;

  public String toString() {
    return toStringHelper().add("key", key).add("value", value).add("comment", comment).toString();
  }
}
