package com.diegohaefliger.atsresumeoptimizer.preference.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.preference.domain.JobPreference;
import org.mapstruct.Mapper;

@Mapper
interface JobPreferenceWebMapper {

	JobPreference toDomain(JobPreferenceRequest request);

	JobPreferenceResponse toResponse(JobPreference preference);
}
