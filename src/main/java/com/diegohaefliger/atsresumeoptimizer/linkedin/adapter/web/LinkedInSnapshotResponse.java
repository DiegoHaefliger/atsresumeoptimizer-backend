package com.diegohaefliger.atsresumeoptimizer.linkedin.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.ProfileSection;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.SnapshotSource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

record LinkedInSnapshotResponse(
		UUID id,
		Instant capturedAt,
		SnapshotSource source,
		LinkedInProfileResponse profile,
		boolean hasPhoto,
		boolean hasBanner,
		List<ProfileSection> missingSections) {
}
