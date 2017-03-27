package com.datagre.apps.omicron.biz.repository;

import com.datagre.apps.omicron.biz.entity.GrayReleaseRule;
import org.springframework.data.repository.PagingAndSortingRepository;
import java.util.List;

public interface GrayReleaseRuleRepository extends PagingAndSortingRepository<GrayReleaseRule, Long> {

  GrayReleaseRule findTopByAppIdAndClusterNameAndNamespaceNameAndBranchNameOrderByIdDesc(String appId, String clusterName,
                                                                                         String namespaceName, String branchName);

  List<GrayReleaseRule> findByAppIdAndClusterNameAndNamespaceName(String appId,
                                                                  String clusterName, String namespaceName);

  List<GrayReleaseRule> findFirst500ByIdGreaterThanOrderByIdAsc(Long id);

}