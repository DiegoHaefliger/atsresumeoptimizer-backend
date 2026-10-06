package com.diegohaefliger.atsresumeoptimizer.selection.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionProcess;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionProcessData;
import org.mapstruct.Mapper;

@Mapper
interface SelectionProcessWebMapper {

	SelectionProcessData toData(SelectionProcessRequest request);

	SelectionProcessResponse toResponse(SelectionProcess process);
}
