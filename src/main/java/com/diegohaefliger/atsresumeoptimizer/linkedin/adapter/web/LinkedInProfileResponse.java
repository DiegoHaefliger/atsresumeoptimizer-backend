package com.diegohaefliger.atsresumeoptimizer.linkedin.adapter.web;

import java.util.List;

record LinkedInProfileResponse(
		String profileUrl,
		String fullName,
		String headline,
		String location,
		String about,
		String customUrl,
		String connections,
		String followers,
		List<LinkedInEntryResponse> experiences,
		List<LinkedInEntryResponse> educations,
		List<LinkedInEntryResponse> certifications,
		List<LinkedInEntryResponse> projects,
		List<LinkedInEntryResponse> publications,
		List<String> skills,
		List<String> languages,
		List<String> recommendations,
		List<String> featured,
		List<String> recentActivity) {
}
