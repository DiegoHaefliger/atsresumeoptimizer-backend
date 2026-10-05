package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import org.mapstruct.Mapper;

@Mapper
interface AnalysisJobViewMapper {

	AnalysisJobView toView(JobOffer offer);
}
