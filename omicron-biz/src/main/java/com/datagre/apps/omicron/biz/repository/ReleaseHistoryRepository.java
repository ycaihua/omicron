package com.datagre.apps.omicron.biz.repository;

import com.datagre.apps.omicron.biz.entity.ReleaseHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Created by zengxiaobo on 2017/3/27.
 */
public interface ReleaseHistoryRepository extends PagingAndSortingRepository<ReleaseHistory, Long> {
  Page<ReleaseHistory> findByAppIdAndClusterNameAndNamespaceNameOrderByIdDesc(String appId, String
          clusterName, String namespaceName, Pageable pageable);

  Page<ReleaseHistory> findByReleaseIdAndOperationOrderByIdDesc(long releaseId, int operation, Pageable pageable);

  Page<ReleaseHistory> findByPreviousReleaseIdAndOperationOrderByIdDesc(long previousReleaseId, int operation, Pageable pageable);

  @Modifying
  @Query("update ReleaseHistory set isdeleted=1,DataChange_LastModifiedBy = ?4 where appId=?1 and clusterName=?2 and namespaceName = ?3")
  int batchDelete(String appId, String clusterName, String namespaceName, String operator);

}
