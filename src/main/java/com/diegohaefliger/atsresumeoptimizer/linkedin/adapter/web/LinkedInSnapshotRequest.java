package com.diegohaefliger.atsresumeoptimizer.linkedin.adapter.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record LinkedInSnapshotRequest(
		@NotNull @Valid LinkedInProfileRequest profile,
		@Size(max = 4_200_000) String photoBase64,
		@Size(max = 30) String photoMimeType,
		@Size(max = 4_200_000) String bannerBase64,
		@Size(max = 30) String bannerMimeType) {
}
