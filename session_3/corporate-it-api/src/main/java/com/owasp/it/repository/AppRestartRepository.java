package com.owasp.it.repository;

import com.owasp.it.domain.AppRestart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AppRestartRepository extends JpaRepository<AppRestart, Integer> {

    List<AppRestart> findByAppNameIgnoreCaseOrderByOperationDateDesc(String appName);
}
