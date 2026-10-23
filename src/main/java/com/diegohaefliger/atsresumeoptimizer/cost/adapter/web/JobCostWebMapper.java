package com.diegohaefliger.atsresumeoptimizer.cost.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.cost.domain.JobCost;
import org.mapstruct.Mapper;

@Mapper
interface JobCostWebMapper {

	JobCostResponse toResponse(JobCost cost);
}
