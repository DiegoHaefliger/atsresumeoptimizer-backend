package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AnalysisDimensionRepository extends JpaRepository<AnalysisDimensionEntity, UUID> {

	List<AnalysisDimensionEntity> findByAnalysisId(UUID analysisId);
}
