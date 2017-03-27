package com.datagre.apps.omicron.common.entity;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import lombok.Data;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;
@Data
@MappedSuperclass
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public abstract class BaseEntity {

  @Id
  @GeneratedValue
  @Column(name = "Id")
  private long id;

  @Column(name = "IsDeleted", columnDefinition = "Bit default '0'")
  protected boolean isDeleted = false;

  @Column(name = "DataChange_CreatedBy", nullable = false)
  private String dataChangeCreatedBy;

  @Column(name = "DataChange_CreatedTime", nullable = false)
  private Date dataChangeCreatedTime;

  @Column(name = "DataChange_LastModifiedBy")
  private String dataChangeLastModifiedBy;

  @Column(name = "DataChange_LastTime")
  private Date dataChangeLastModifiedTime;

  @PrePersist
  protected void prePersist() {
    if (this.dataChangeCreatedTime == null) dataChangeCreatedTime = new Date();
    if (this.dataChangeLastModifiedTime == null) dataChangeLastModifiedTime = new Date();
  }

  @PreUpdate
  protected void preUpdate() {
    this.dataChangeLastModifiedTime = new Date();
  }

  @PreRemove
  protected void preRemove() {
    this.dataChangeLastModifiedTime = new Date();
  }

  protected ToStringHelper toStringHelper() {
    return MoreObjects.toStringHelper(this).omitNullValues().add("id", id)
        .add("dataChangeCreatedBy", dataChangeCreatedBy)
        .add("dataChangeCreatedTime", dataChangeCreatedTime)
        .add("dataChangeLastModifiedBy", dataChangeLastModifiedBy)
        .add("dataChangeLastModifiedTime", dataChangeLastModifiedTime);
  }

  public String toString(){
    return toStringHelper().toString();
  }
}
