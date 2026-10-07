package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface RewriteRepository extends JpaRepository<RewriteEntity, UUID> {

	Optional<RewriteEntity> findFirstByAnalysisIdOrderByCreatedAtDesc(UUID analysisId);
}
