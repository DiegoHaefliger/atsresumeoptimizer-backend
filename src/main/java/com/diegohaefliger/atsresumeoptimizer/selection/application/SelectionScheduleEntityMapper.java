package com.diegohaefliger.atsresumeoptimizer.selection.application;

import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionSchedule;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionScheduleData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
interface SelectionScheduleEntityMapper {

	SelectionSchedule toDomain(SelectionScheduleEntity entity);

	@Mapping(target = "id", ignore = true)
	@Mapping(target = "processId", ignore = true)
	@Mapping(target = "stage", ignore = true)
	@Mapping(target = "status", ignore = true)
	@Mapping(target = "createdAt", ignore = true)
	@Mapping(target = "updatedAt", ignore = true)
	void update(SelectionScheduleData data, @MappingTarget SelectionScheduleEntity entity);
}
