package com.datagre.apps.omicron.common.utils;

import com.datagre.apps.omicron.common.dto.GrayReleaseRuleItemDTO;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * Created by zengxiaobo on 2017/3/24.
 */
public class GrayReleaseRuleItemTransformer {
  private static final Gson gson = new Gson();
  private static final Type grayReleaseRuleItemsType = new TypeToken<Set<GrayReleaseRuleItemDTO>>() {
  }.getType();

  public static Set<GrayReleaseRuleItemDTO> batchTransformFromJSON(String content) {
    return gson.fromJson(content, grayReleaseRuleItemsType);
  }

  public static String batchTransformToJSON(Set<GrayReleaseRuleItemDTO> ruleItems) {
    return gson.toJson(ruleItems);
  }
}
