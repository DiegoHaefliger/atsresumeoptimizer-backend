package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface FindingRepository extends JpaRepository<FindingEntity, UUID> {

	List<FindingEntity> findByAnalysisId(UUID analysisId);

	@Modifying
	@Query("delete from FindingEntity e where e.analysisId = :analysisId")
	void deleteByAnalysisId(@Param("analysisId") UUID analysisId);
}
