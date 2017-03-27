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
@Table(name = "Audit")
@SQLDelete(sql = "Update Audit set isDeleted = 1 where id = ?")
@Where(clause = "isDeleted = 0")
public class Audit extends BaseEntity {

  public enum OP {
    INSERT, UPDATE, DELETE
  }

  @Column(name = "EntityName", nullable = false)
  private String entityName;

  @Column(name = "EntityId")
  private Long entityId;

  @Column(name = "OpName", nullable = false)
  private String opName;

  @Column(name = "Comment")
  private String comment;

  public String toString() {
    return toStringHelper().add("entityName", entityName).add("entityId", entityId)
        .add("opName", opName).add("comment", comment).toString();
  }
}
