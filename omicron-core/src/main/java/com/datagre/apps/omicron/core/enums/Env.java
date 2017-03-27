package com.datagre.apps.omicron.core.enums;

import com.google.common.base.Preconditions;

/**
 * Created by zengxiaobo on 2017/3/24.
 */
public enum Env{
  LOCAL, DEV, FWS, FAT, UAT, LPT, PRO, TOOLS;

  public static Env fromString(String env) {
    Env environment = EnvUtils.transformEnv(env);
    Preconditions.checkArgument(environment != null, String.format("Env %s is invalid", env));
    return environment;
  }
}
