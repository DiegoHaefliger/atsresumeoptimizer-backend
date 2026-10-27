package com.diegohaefliger.atsresumeoptimizer.linkedin.adapter.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

record LinkedInProfileRequest(
		@Size(max = 300) String profileUrl,
		@Size(max = 200) String fullName,
		@Size(max = 500) String headline,
		@Size(max = 200) String location,
		@Size(max = 6000) String about,
		@Size(max = 300) String customUrl,
		@Size(max = 50) String connections,
		@Size(max = 50) String followers,
		@Size(max = 60) List<@Valid LinkedInEntryRequest> experiences,
		@Size(max = 30) List<@Valid LinkedInEntryRequest> educations,
		@Size(max = 60) List<@Valid LinkedInEntryRequest> certifications,
		@Size(max = 60) List<@Valid LinkedInEntryRequest> projects,
		@Size(max = 60) List<@Valid LinkedInEntryRequest> publications,
		@Size(max = 100) List<@Size(max = 200) String> skills,
		@Size(max = 30) List<@Size(max = 200) String> languages,
		@Size(max = 50) List<@Size(max = 3000) String> recommendations,
		@Size(max = 30) List<@Size(max = 1000) String> featured,
		@Size(max = 50) List<@Size(max = 3000) String> recentActivity) {
}
