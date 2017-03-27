package com.datagre.apps.omicron.common.dto;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;

/**
 * storage cud result
 */
@Data
public class ItemChangeSets extends BaseDTO{

  private List<ItemDTO> createItems = new LinkedList<>();
  private List<ItemDTO> updateItems = new LinkedList<>();
  private List<ItemDTO> deleteItems = new LinkedList<>();

  public void addCreateItem(ItemDTO item) {
    createItems.add(item);
  }

  public void addUpdateItem(ItemDTO item) {
    updateItems.add(item);
  }

  public void addDeleteItem(ItemDTO item) {
    deleteItems.add(item);
  }

  public boolean isEmpty(){
    return createItems.isEmpty() && updateItems.isEmpty() && deleteItems.isEmpty();
  }
}
