package com.diegohaefliger.atsresumeoptimizer.ai;

import java.util.List;

public record LinkedInAnalysisBrief(String careerProfile, String linkedInProfile, List<ChatImage> images) {

	public LinkedInAnalysisBrief {
		images = images == null ? List.of() : List.copyOf(images);
	}
}
