package com.diegohaefliger.atsresumeoptimizer.job.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.job.JobDetails;
import com.diegohaefliger.atsresumeoptimizer.job.JobRegistration;
import org.mapstruct.Mapper;

@Mapper
interface CreateJobRequestMapper {

	JobDetails toJobDetails(CreateJobRequest request);

	JobCreatedResponse toResponse(JobRegistration registration);
}
