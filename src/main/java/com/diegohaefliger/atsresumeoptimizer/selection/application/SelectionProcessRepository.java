package com.diegohaefliger.atsresumeoptimizer.selection.application;

import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SelectionProcessRepository extends JpaRepository<SelectionProcessEntity, UUID> {

	List<SelectionProcessEntity> findAllByOrderByUpdatedAtDesc();

	List<SelectionProcessEntity> findByStageOrderByUpdatedAtDesc(SelectionStage stage);
}
