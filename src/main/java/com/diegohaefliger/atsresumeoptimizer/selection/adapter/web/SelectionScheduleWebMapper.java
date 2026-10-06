package com.diegohaefliger.atsresumeoptimizer.selection.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionSchedule;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionScheduleData;
import org.mapstruct.Mapper;

@Mapper
interface SelectionScheduleWebMapper {

	SelectionScheduleData toData(SelectionScheduleRequest request);

	SelectionScheduleResponse toResponse(SelectionSchedule schedule);
}
