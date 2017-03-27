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
@Table(name = "Item")
@SQLDelete(sql = "Update Item set isDeleted = 1 where id = ?")
@Where(clause = "isDeleted = 0")
public class Item extends BaseEntity {

  @Column(name = "NamespaceId", nullable = false)
  private long namespaceId;

  @Column(name = "key", nullable = false)
  private String key;

  @Column(name = "value")
  @Lob
  private String value;

  @Column(name = "comment")
  private String comment;

  @Column(name = "LineNum")
  private Integer lineNum;

  public String toString() {
    return toStringHelper().add("namespaceId", namespaceId).add("key", key).add("value", value)
        .add("lineNum", lineNum).add("comment", comment).toString();
  }
}
