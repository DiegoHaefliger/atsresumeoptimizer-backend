package com.diegohaefliger.atsresumeoptimizer.linkedin.adapter.web;

import jakarta.validation.constraints.Size;

record LinkedInEntryRequest(
		@Size(max = 300) String title,
		@Size(max = 300) String organization,
		@Size(max = 100) String period,
		@Size(max = 200) String location,
		@Size(max = 6000) String description) {
}
