package com.diegohaefliger.atsresumeoptimizer.analysis.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.job.JobDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface JobDetailsMapper {

	@Mapping(target = "company", source = "jobCompany")
	@Mapping(target = "sourceUrl", source = "jobUrl")
	@Mapping(target = "workModel", source = "jobWorkModel")
	@Mapping(target = "interviewUrl", ignore = true)
	@Mapping(target = "salary", ignore = true)
	@Mapping(target = "benefits", ignore = true)
	@Mapping(target = "title", ignore = true)
	@Mapping(target = "seniority", ignore = true)
	@Mapping(target = "contractType", ignore = true)
	JobDetails toJobDetails(JobDetailsParams params);
}
