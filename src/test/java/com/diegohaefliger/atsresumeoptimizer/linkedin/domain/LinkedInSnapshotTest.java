package com.diegohaefliger.atsresumeoptimizer.linkedin.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LinkedInSnapshotTest {

	private static LinkedInProfile profile(String headline, String about, List<LinkedInEntry> experiences) {
		return new LinkedInProfile(null, "Ana", headline, null, about, null, null, null, experiences, null, null, null,
				null, List.of("Java"), null, null, null, null);
	}

	private static LinkedInSnapshot snapshot(LinkedInProfile profile, byte[] photo) {
		return new LinkedInSnapshot(UUID.randomUUID(), Instant.now(), SnapshotSource.EXTENSION, profile, photo,
				photo == null ? null : "image/png", null, null);
	}

	@Test
	void listsEverySectionThatWasNotCaptured() {
		var entry = new LinkedInEntry("Dev", "Acme", null, null, null);
		var result = snapshot(profile("Dev Java", null, List.of(entry)), new byte[] {1});

		assertThat(result.missingSections()).doesNotContain(ProfileSection.PHOTO, ProfileSection.HEADLINE,
				ProfileSection.EXPERIENCE, ProfileSection.SKILLS);
		assertThat(result.missingSections()).contains(ProfileSection.BANNER, ProfileSection.ABOUT,
				ProfileSection.RECOMMENDATIONS, ProfileSection.ACTIVITY);
	}

	@Test
	void reportsThePhotoAsMissingWhenNoneWasSent() {
		var result = snapshot(profile("Dev Java", "Sobre", List.of()), null);

		assertThat(result.hasPhoto()).isFalse();
		assertThat(result.missingSections()).contains(ProfileSection.PHOTO, ProfileSection.EXPERIENCE);
	}

	@Test
	void dropsBlankEntriesAndDuplicatedTexts() {
		var cleaned = new LinkedInProfile(null, null, "  ", null, null, null, null, null,
				List.of(new LinkedInEntry(" ", null, null, null, "sem título")), null, null, null, null,
				java.util.Arrays.asList(" Java ", "Java", "", null), null, null, null, null);

		assertThat(cleaned.headline()).isNull();
		assertThat(cleaned.experiences()).isEmpty();
		assertThat(cleaned.skills()).containsExactly("Java");
		assertThat(cleaned.lacksContent()).isFalse();
	}

	@Test
	void rejectsImagesInUnsupportedFormatsOrTooBig() {
		assertThatThrownBy(() -> new LinkedInImage(new byte[] {1}, "image/gif"))
				.isInstanceOf(InvalidLinkedInSnapshotException.class);
		assertThatThrownBy(() -> new LinkedInImage(new byte[LinkedInImage.MAX_BYTES + 1], "image/png"))
				.isInstanceOf(InvalidLinkedInSnapshotException.class);
		assertThatThrownBy(() -> new LinkedInImage(new byte[0], "image/png"))
				.isInstanceOf(InvalidLinkedInSnapshotException.class);
	}
}
