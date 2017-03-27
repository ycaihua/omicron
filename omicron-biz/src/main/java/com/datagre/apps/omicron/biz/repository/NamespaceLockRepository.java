package com.datagre.apps.omicron.biz.repository;

import com.datagre.apps.omicron.biz.entity.NamespaceLock;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface NamespaceLockRepository extends PagingAndSortingRepository<NamespaceLock, Long> {

  NamespaceLock findByNamespaceId(Long namespaceId);

  Long deleteByNamespaceId(Long namespaceId);

}
