package com.diegohaefliger.atsresumeoptimizer.careerprofile.application;

import com.diegohaefliger.atsresumeoptimizer.careerprofile.CareerProfile;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper
interface CareerProfileEntityMapper {

	CareerProfile toDomain(CareerProfileEntity entity);

	@Mapping(target = "updatedAt", ignore = true)
	void update(CareerProfile profile, @MappingTarget CareerProfileEntity entity);
}
