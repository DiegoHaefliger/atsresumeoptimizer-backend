package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSectionReview;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.ProfileSection;
import java.util.List;
import org.junit.jupiter.api.Test;

class LinkedInOverallScoreTest {

	private static LinkedInSectionReview review(ProfileSection section, int score) {
		return new LinkedInSectionReview(section, score, "d", List.of(), null, List.of());
	}

	@Test
	void weighsHeadlineAboutExperienceAndSkillsMore() {
		var sections = List.of(review(ProfileSection.HEADLINE, 90), review(ProfileSection.ACTIVITY, 30));

		assertThat(LinkedInOverallScore.of(sections, true)).hasValue(75);
	}

	@Test
	void ignoresImagesThatWereNotAnalyzed() {
		var sections = List.of(review(ProfileSection.ABOUT, 80), review(ProfileSection.PHOTO, 0), review(ProfileSection.BANNER, 0));

		assertThat(LinkedInOverallScore.of(sections, false)).hasValue(80);
		assertThat(LinkedInOverallScore.of(sections, true)).hasValue(48);
	}

	@Test
	void isEmptyWithoutScorableSections() {
		assertThat(LinkedInOverallScore.of(List.of(review(ProfileSection.PHOTO, 0)), false)).isEmpty();
	}
}
