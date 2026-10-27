package com.diegohaefliger.atsresumeoptimizer.careerprofile.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.careerprofile.domain.ProfileTone;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

record CareerProfileRequest(
		@NotBlank @Size(max = 150) String areaOfExpertise,
		@Size(max = 150) String currentJobTitle,
		@Size(max = 50) String currentSeniority,
		@NotBlank @Size(max = 150) String targetJobTitle,
		@Size(max = 50) String targetSeniority,
		@Size(max = 100) String targetTimeframe,
		@Size(max = 20) List<@Size(max = 100) String> targetIndustries,
		@Size(max = 30) List<@Size(max = 150) String> targetCompanies,
		@Size(max = 150) String location,
		@Size(max = 20) List<@Size(max = 100) String> languages,
		@Size(max = 20) List<@Size(max = 150) String> strengths,
		@Size(max = 2000) String differentiators,
		ProfileTone tone,
		@Size(max = 10) String profileLanguage,
		@Size(max = 2000) String notes) {
}
