package com.diegohaefliger.atsresumeoptimizer.careerprofile.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.careerprofile.ProfileTone;
import java.util.List;

record CareerProfileResponse(
		String areaOfExpertise,
		String currentJobTitle,
		String currentSeniority,
		String targetJobTitle,
		String targetSeniority,
		String targetTimeframe,
		List<String> targetIndustries,
		List<String> targetCompanies,
		String location,
		List<String> languages,
		List<String> strengths,
		String differentiators,
		ProfileTone tone,
		String profileLanguage,
		String notes) {
}
