package com.datagre.apps.omicron.core.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
/**
 * Created by zengxiaobo on 2017/3/24.
 */
public class DNSUtil {

  public static List<String> resolve(String domainName) throws UnknownHostException {
    List<String> result = new ArrayList<String>();

    InetAddress[] addresses = InetAddress.getAllByName(domainName);
    if (addresses != null) {
      for (InetAddress addr : addresses) {
        result.add(addr.getHostAddress());
      }
    }

    return result;
  }

}
