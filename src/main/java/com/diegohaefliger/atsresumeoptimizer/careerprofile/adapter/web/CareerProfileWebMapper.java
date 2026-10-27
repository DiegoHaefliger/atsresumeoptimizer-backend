package com.diegohaefliger.atsresumeoptimizer.careerprofile.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.careerprofile.domain.CareerProfile;
import org.mapstruct.Mapper;

@Mapper
interface CareerProfileWebMapper {

	CareerProfile toDomain(CareerProfileRequest request);

	CareerProfileResponse toResponse(CareerProfile profile);
}
