package com.datagre.apps.omicron.biz.entity;

import com.datagre.apps.omicron.common.entity.BaseEntity;
import lombok.Data;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
/**
 * Created by zengxiaobo on 2017/3/27.
 */
@Data
@Entity
@Table(name = "NamespaceLock")
@Where(clause = "isDeleted = 0")
public class NamespaceLock extends BaseEntity {

  @Column(name = "NamespaceId")
  private long namespaceId;
}
