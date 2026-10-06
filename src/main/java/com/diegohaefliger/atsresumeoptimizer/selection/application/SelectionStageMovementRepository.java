package com.diegohaefliger.atsresumeoptimizer.selection.application;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SelectionStageMovementRepository extends JpaRepository<SelectionStageMovementEntity, UUID> {

	List<SelectionStageMovementEntity> findByProcessIdOrderByMovedAtAsc(UUID processId);
}
