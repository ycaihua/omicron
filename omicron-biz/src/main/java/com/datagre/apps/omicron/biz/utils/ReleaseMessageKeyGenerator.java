package com.datagre.apps.omicron.biz.utils;

import com.datagre.apps.omicron.core.ConfigConsts;
import com.google.common.base.Joiner;

public class ReleaseMessageKeyGenerator {

  private static final Joiner STRING_JOINER = Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR);

  public static String generate(String appId, String cluster, String namespace) {
    return STRING_JOINER.join(appId, cluster, namespace);
  }
}
