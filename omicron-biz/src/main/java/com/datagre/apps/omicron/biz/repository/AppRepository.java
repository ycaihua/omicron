package com.datagre.apps.omicron.biz.repository;

import com.datagre.apps.omicron.common.entity.App;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AppRepository extends PagingAndSortingRepository<App, Long> {

  @Query("SELECT a from App a WHERE a.name LIKE %:name%")
  List<App> findByName(@Param("name") String name);

  App findByAppId(String appId);

}
