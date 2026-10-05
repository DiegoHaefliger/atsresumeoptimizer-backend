package com.diegohaefliger.atsresumeoptimizer.preference.application;

import com.diegohaefliger.atsresumeoptimizer.preference.domain.JobPreference;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
interface JobPreferenceEntityMapper {

	JobPreference toDomain(JobPreferenceEntity entity);

	@Mapping(target = "updatedAt", ignore = true)
	void update(JobPreference preference, @MappingTarget JobPreferenceEntity entity);
}
