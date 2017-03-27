package com.datagre.apps.omicron.common.constants;

import com.datagre.apps.omicron.common.dto.GrayReleaseRuleItemDTO;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public interface GsonType {

  Type CONFIG = new TypeToken<Map<String, String>>() {}.getType();

  Type RULE_ITEMS = new TypeToken<List<GrayReleaseRuleItemDTO>>() {}.getType();

}
