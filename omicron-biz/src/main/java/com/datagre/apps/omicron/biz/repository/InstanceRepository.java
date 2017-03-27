package com.datagre.apps.omicron.biz.repository;

import com.datagre.apps.omicron.biz.entity.Instance;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface InstanceRepository extends PagingAndSortingRepository<Instance, Long> {
  Instance findByAppIdAndClusterNameAndDataCenterAndIp(String appId, String clusterName, String dataCenter, String ip);
}
