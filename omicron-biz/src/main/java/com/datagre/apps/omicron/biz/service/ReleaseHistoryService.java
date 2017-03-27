package com.datagre.apps.omicron.biz.service;

import com.datagre.apps.omicron.biz.entity.Audit;
import com.datagre.apps.omicron.biz.entity.ReleaseHistory;
import com.datagre.apps.omicron.biz.repository.ReleaseHistoryRepository;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.Map;

/**
 * Created by zengxiaobo on 2017/3/27.
 */
@Service
public class ReleaseHistoryService {
  private Gson gson = new Gson();

  @Autowired
  private ReleaseHistoryRepository releaseHistoryRepository;
  @Autowired
  private AuditService auditService;


  public Page<ReleaseHistory> findReleaseHistoriesByNamespace(String appId, String clusterName,
                                                              String namespaceName, Pageable
                                                                  pageable) {
    return releaseHistoryRepository.findByAppIdAndClusterNameAndNamespaceNameOrderByIdDesc(appId, clusterName,
                                                                                           namespaceName, pageable);
  }

  public Page<ReleaseHistory> findByReleaseIdAndOperation(long releaseId, int operation, Pageable page) {
    return releaseHistoryRepository.findByReleaseIdAndOperationOrderByIdDesc(releaseId, operation, page);
  }

  public Page<ReleaseHistory> findByPreviousReleaseIdAndOperation(long previousReleaseId, int operation, Pageable page) {
    return releaseHistoryRepository.findByPreviousReleaseIdAndOperationOrderByIdDesc(previousReleaseId, operation, page);
  }

  @Transactional
  public ReleaseHistory createReleaseHistory(String appId, String clusterName, String
      namespaceName, String branchName, long releaseId, long previousReleaseId, int operation,
                                             Map<String, Object> operationContext, String operator) {
    ReleaseHistory releaseHistory = new ReleaseHistory();
    releaseHistory.setAppId(appId);
    releaseHistory.setClusterName(clusterName);
    releaseHistory.setNamespaceName(namespaceName);
    releaseHistory.setBranchName(branchName);
    releaseHistory.setReleaseId(releaseId);
    releaseHistory.setPreviousReleaseId(previousReleaseId);
    releaseHistory.setOperation(operation);
    if (operationContext == null) {
      releaseHistory.setOperationContext("{}"); //default empty object
    } else {
      releaseHistory.setOperationContext(gson.toJson(operationContext));
    }
    releaseHistory.setDataChangeCreatedTime(new Date());
    releaseHistory.setDataChangeCreatedBy(operator);
    releaseHistory.setDataChangeLastModifiedBy(operator);

    releaseHistoryRepository.save(releaseHistory);

    auditService.audit(ReleaseHistory.class.getSimpleName(), releaseHistory.getId(),
                       Audit.OP.INSERT, releaseHistory.getDataChangeCreatedBy());

    return releaseHistory;
  }

  @Transactional
  public int batchDelete(String appId, String clusterName, String namespaceName, String operator) {
    return releaseHistoryRepository.batchDelete(appId, clusterName, namespaceName, operator);
  }
}
