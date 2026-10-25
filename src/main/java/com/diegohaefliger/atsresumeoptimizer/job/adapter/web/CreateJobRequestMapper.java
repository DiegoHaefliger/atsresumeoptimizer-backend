package com.diegohaefliger.atsresumeoptimizer.job.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.job.JobDetails;
import com.diegohaefliger.atsresumeoptimizer.job.JobRegistration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface CreateJobRequestMapper {

	@Mapping(target = "origin", ignore = true)
	JobDetails toJobDetails(CreateJobRequest request);

	JobCreatedResponse toResponse(JobRegistration registration);
}
