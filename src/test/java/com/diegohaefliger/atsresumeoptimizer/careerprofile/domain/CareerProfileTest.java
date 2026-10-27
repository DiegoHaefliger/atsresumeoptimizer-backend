package com.diegohaefliger.atsresumeoptimizer.careerprofile.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class CareerProfileTest {

	private static CareerProfile profile(String area, String target, List<String> strengths) {
		return new CareerProfile(area, null, null, target, null, null, null, null, null, null, strengths, null, null, null,
				null);
	}

	@Test
	void requiresTheAreaOfExpertise() {
		assertThatThrownBy(() -> profile("  ", "Tech Lead", List.of()))
				.isInstanceOf(InvalidCareerProfileException.class)
				.hasMessageContaining("área de atuação");
	}

	@Test
	void requiresTheTargetJobTitle() {
		assertThatThrownBy(() -> profile("Engenharia de software", null, List.of()))
				.isInstanceOf(InvalidCareerProfileException.class)
				.hasMessageContaining("próximo cargo");
	}

	@Test
	void cleansTextAndListsAndAppliesDefaults() {
		CareerProfile result = profile("  Engenharia de software ", "Tech Lead", java.util.Arrays.asList(" Java ", "", null, "Java"));

		assertThat(result.areaOfExpertise()).isEqualTo("Engenharia de software");
		assertThat(result.strengths()).containsExactly("Java");
		assertThat(result.targetCompanies()).isEmpty();
		assertThat(result.tone()).isEqualTo(ProfileTone.TECHNICAL);
		assertThat(result.profileLanguage()).isEqualTo("pt-BR");
	}
}
