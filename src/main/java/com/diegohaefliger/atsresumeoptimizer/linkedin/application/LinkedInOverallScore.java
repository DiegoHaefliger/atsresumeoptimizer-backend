package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSectionReview;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.ProfileSection;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

final class LinkedInOverallScore {

	private static final int DEFAULT_WEIGHT = 1;
	private static final Map<ProfileSection, Integer> WEIGHTS = Map.of(
			ProfileSection.HEADLINE, 3,
			ProfileSection.ABOUT, 3,
			ProfileSection.EXPERIENCE, 3,
			ProfileSection.SKILLS, 2);

	private LinkedInOverallScore() {
	}

	static OptionalInt of(List<LinkedInSectionReview> sections, boolean imagesAnalyzed) {
		int weighted = 0;
		int totalWeight = 0;
		for (LinkedInSectionReview review : sections) {
			if (!imagesAnalyzed && isImage(review.section())) {
				continue;
			}
			int weight = WEIGHTS.getOrDefault(review.section(), DEFAULT_WEIGHT);
			weighted += review.score() * weight;
			totalWeight += weight;
		}
		return totalWeight == 0 ? OptionalInt.empty() : OptionalInt.of(Math.round((float) weighted / totalWeight));
	}

	private static boolean isImage(ProfileSection section) {
		return section == ProfileSection.PHOTO || section == ProfileSection.BANNER;
	}
}
