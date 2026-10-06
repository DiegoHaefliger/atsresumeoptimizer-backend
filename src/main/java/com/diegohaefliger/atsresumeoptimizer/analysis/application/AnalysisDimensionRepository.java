package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AnalysisDimensionRepository extends JpaRepository<AnalysisDimensionEntity, UUID> {

	List<AnalysisDimensionEntity> findByAnalysisId(UUID analysisId);

	@Modifying
	@Query("delete from AnalysisDimensionEntity e where e.analysisId = :analysisId")
	void deleteByAnalysisId(@Param("analysisId") UUID analysisId);
}
