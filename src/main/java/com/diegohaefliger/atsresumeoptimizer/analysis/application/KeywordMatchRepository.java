package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface KeywordMatchRepository extends JpaRepository<KeywordMatchEntity, UUID> {

	List<KeywordMatchEntity> findByAnalysisId(UUID analysisId);
}
