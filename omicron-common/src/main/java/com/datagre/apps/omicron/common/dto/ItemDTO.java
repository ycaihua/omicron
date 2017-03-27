package com.datagre.apps.omicron.common.dto;

import lombok.Data;

@Data
public class ItemDTO extends BaseDTO{

  private long id;

  private long namespaceId;

  private String key;

  private String value;

  private String comment;

  private int lineNum;

  public ItemDTO() {

  }

  public ItemDTO(String key, String value, String comment, int lineNum) {
    this.key = key;
    this.value = value;
    this.comment = comment;
    this.lineNum = lineNum;
  }
}
