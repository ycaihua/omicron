package com.datagre.apps.omicron.biz.grayReleaseRule;

import com.datagre.apps.omicron.common.dto.GrayReleaseRuleItemDTO;
import lombok.Data;

import java.util.Set;

/**
 * Created by zengxiaobo on 2017/3/27.
 */
@Data
public class GrayReleaseRuleCache {
  public long ruleId;
  private String branchName;
  private String namespaceName;
  private long releaseId;
  private long loadVersion;
  private int branchStatus;
  private Set<GrayReleaseRuleItemDTO> ruleItems;
    public GrayReleaseRuleCache(long ruleId, String branchName, String namespaceName, long
            releaseId, int branchStatus, long loadVersion, Set<GrayReleaseRuleItemDTO> ruleItems) {
        this.ruleId = ruleId;
        this.branchName = branchName;
        this.namespaceName = namespaceName;
        this.releaseId = releaseId;
        this.branchStatus = branchStatus;
        this.loadVersion = loadVersion;
        this.ruleItems = ruleItems;
    }
  public boolean matches(String clientAppId, String clientIp) {
    for (GrayReleaseRuleItemDTO ruleItem : ruleItems) {
      if (ruleItem.matches(clientAppId, clientIp)) {
        return true;
      }
    }
    return false;
  }
}
