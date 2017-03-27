package com.datagre.apps.omicron.common.controller;

import com.datagre.apps.omicron.Omicron;
import com.datagre.framework.foundation.Foundation;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/omicron")
public class OmicronInfoController {

  @RequestMapping("app")
  public String getApp() {
    return Foundation.app().toString();
  }

  @RequestMapping("web")
  public String getEnv() {
    return Foundation.web().toString();
  }

  @RequestMapping("net")
  public String getNet() {
    return Foundation.net().toString();
  }

  @RequestMapping("server")
  public String getServer() {
    return Foundation.server().toString();
  }

  @RequestMapping("version")
  public String getVersion() {
    return Omicron.VERSION;
  }
}
