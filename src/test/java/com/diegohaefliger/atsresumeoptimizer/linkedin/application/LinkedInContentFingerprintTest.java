package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInEntry;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInProfile;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.ProfileSection;
import java.util.List;
import org.junit.jupiter.api.Test;

class LinkedInContentFingerprintTest {

	private static LinkedInProfile profile(String about, List<String> skills, String followers) {
		return new LinkedInProfile(null, "Ana", "Dev Java", "SP", about, null, "500+", followers,
				List.of(new LinkedInEntry("Dev", "Acme", "2020", null, "fiz X")), null, null, null, null, skills, null,
				null, null, null);
	}

	@Test
	void sameContentHasTheSameFingerprintRegardlessOfFollowers() {
		var before = LinkedInContentFingerprint.of(profile("Sobre", List.of("Java"), "100"), null, null);
		var after = LinkedInContentFingerprint.of(profile("Sobre", List.of("Java"), "999"), null, null);

		assertThat(LinkedInContentFingerprint.changed(before, after)).isEmpty();
	}

	@Test
	void listsEachSectionThatChanged() {
		var before = LinkedInContentFingerprint.of(profile("Sobre", List.of("Java"), null), null, null);
		var after = LinkedInContentFingerprint.of(profile("Sobre novo", List.of("Java", "Spring"), null), new byte[] {1}, null);

		assertThat(LinkedInContentFingerprint.changed(before, after))
				.containsExactly(ProfileSection.PHOTO, ProfileSection.ABOUT, ProfileSection.SKILLS);
	}

	@Test
	void detectsAnEditInsideAnExperienceDescription() {
		var edited = new LinkedInProfile(null, "Ana", "Dev Java", "SP", "Sobre", null, null, null,
				List.of(new LinkedInEntry("Dev", "Acme", "2020", null, "fiz X e Y")), null, null, null, null,
				List.of("Java"), null, null, null, null);

		assertThat(LinkedInContentFingerprint.changed(
				LinkedInContentFingerprint.of(profile("Sobre", List.of("Java"), null), null, null),
				LinkedInContentFingerprint.of(edited, null, null))).containsExactly(ProfileSection.EXPERIENCE);
	}
}
