package com.diegohaefliger.atsresumeoptimizer.scoring.application;

import com.diegohaefliger.atsresumeoptimizer.scoring.domain.AnalysisMode;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ScoringProfileRepository extends JpaRepository<ScoringProfileEntity, UUID> {

	Optional<ScoringProfileEntity> findFirstByModeAndActiveTrueOrderByVersionDesc(AnalysisMode mode);
}
