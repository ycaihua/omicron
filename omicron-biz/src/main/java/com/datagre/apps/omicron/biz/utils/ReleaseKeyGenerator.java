package com.datagre.apps.omicron.biz.utils;

import com.datagre.apps.omicron.biz.entity.Namespace;
import com.datagre.apps.omicron.common.utils.UniqueKeyGenerator;

/**
 * Created by zengxiaobo on 2017/3/27.
 */
public class ReleaseKeyGenerator extends UniqueKeyGenerator {


  /**
   * Generate the release key in the format: timestamp+appId+cluster+namespace+hash(ipAsInt+counter)
   *
   * @param namespace the namespace of the release
   * @return the unique release key
   */
  public static String generateReleaseKey(Namespace namespace) {
    return generate(namespace.getAppId(), namespace.getClusterName(), namespace.getNamespaceName());
  }
}
