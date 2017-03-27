package com.datagre.apps.omicron.biz.repository;

import com.datagre.apps.omicron.biz.entity.Cluster;
import org.springframework.data.repository.PagingAndSortingRepository;
import java.util.List;

public interface ClusterRepository extends PagingAndSortingRepository<Cluster, Long> {

  List<Cluster> findByAppIdAndParentClusterId(String appId, Long parentClusterId);

  List<Cluster> findByAppId(String appId);

  Cluster findByAppIdAndName(String appId, String name);

  List<Cluster> findByParentClusterId(Long parentClusterId);
}
