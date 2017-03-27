package com.datagre.apps.omicron.biz.entity;

import com.google.common.base.MoreObjects;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by zengxiaobo on 2017/3/27.
 */
@NoArgsConstructor
@Entity
@Table(name = "ReleaseMessage")
public class ReleaseMessage {
  @Id
  @GeneratedValue
  @Column(name = "Id")
  private long id;

  @Column(name = "Message", nullable = false)
  private String message;

  @Column(name = "DataChange_LastTime")
  private Date dataChangeLastModifiedTime;

  @PrePersist
  protected void prePersist() {
    if (this.dataChangeLastModifiedTime == null) {
      dataChangeLastModifiedTime = new Date();
    }
  }

  public ReleaseMessage(String message) {
    this.message = message;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .omitNullValues()
        .add("id", id)
        .add("message", message)
        .add("dataChangeLastModifiedTime", dataChangeLastModifiedTime)
        .toString();
  }
}
