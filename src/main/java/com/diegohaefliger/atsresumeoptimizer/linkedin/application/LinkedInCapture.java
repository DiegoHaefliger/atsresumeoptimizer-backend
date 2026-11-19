package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSnapshot;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.ProfileSection;
import java.util.List;

public record LinkedInCapture(LinkedInSnapshot snapshot, boolean unchanged, List<ProfileSection> changedSections) {

	public LinkedInCapture {
		changedSections = changedSections == null ? List.of() : List.copyOf(changedSections);
	}
}
