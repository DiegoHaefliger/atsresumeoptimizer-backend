package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SearchRunRepository extends JpaRepository<SearchRunEntity, UUID> {

	Optional<SearchRunEntity> findFirstByOrderByStartedAtDesc();

	List<SearchRunEntity> findTop20ByOrderByStartedAtDesc();
}
