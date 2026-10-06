package com.diegohaefliger.atsresumeoptimizer.selection.application;

import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionProcess;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionProcessData;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.StageMovement;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
interface SelectionProcessEntityMapper {

	SelectionProcess toDomain(SelectionProcessEntity entity, List<StageMovement> history);

	StageMovement toDomain(SelectionStageMovementEntity entity);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "stage", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	void update(SelectionProcessData data, @MappingTarget SelectionProcessEntity entity);
}
