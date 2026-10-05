package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface FindingRepository extends JpaRepository<FindingEntity, UUID> {

	List<FindingEntity> findByAnalysisId(UUID analysisId);
}
