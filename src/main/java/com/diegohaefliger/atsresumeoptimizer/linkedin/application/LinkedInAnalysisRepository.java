package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface LinkedInAnalysisRepository extends JpaRepository<LinkedInAnalysisEntity, UUID> {

	Optional<LinkedInAnalysisEntity> findFirstByOrderByCreatedAtDesc();

	List<LinkedInAnalysisHead> findAllByOrderByCreatedAtDesc();
}
