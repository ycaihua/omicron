package com.datagre.apps.omicron.biz.service;

import com.datagre.apps.omicron.biz.entity.NamespaceLock;
import com.datagre.apps.omicron.biz.repository.NamespaceLockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NamespaceLockService {

  @Autowired
  private NamespaceLockRepository namespaceLockRepository;

  public NamespaceLock findLock(Long namespaceId){
    return namespaceLockRepository.findByNamespaceId(namespaceId);
  }


  @Transactional
  public NamespaceLock tryLock(NamespaceLock lock){
    return namespaceLockRepository.save(lock);
  }

  @Transactional
  public void unlock(Long namespaceId){
    namespaceLockRepository.deleteByNamespaceId(namespaceId);
  }
}
