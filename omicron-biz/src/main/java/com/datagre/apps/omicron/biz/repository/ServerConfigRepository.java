package com.datagre.apps.omicron.biz.repository;

import com.datagre.apps.omicron.biz.entity.ServerConfig;
import org.springframework.data.repository.PagingAndSortingRepository;

/**
 * Created by zengxiaobo on 2017/3/27.
 */
public interface ServerConfigRepository extends PagingAndSortingRepository<ServerConfig, Long> {
  ServerConfig findTopByKeyAndCluster(String key, String cluster);
}
