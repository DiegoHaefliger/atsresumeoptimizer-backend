package com.diegohaefliger.atsresumeoptimizer.linkedin.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record LinkedInSnapshot(
		UUID id,
		Instant capturedAt,
		SnapshotSource source,
		LinkedInProfile profile,
		byte[] photo,
		String photoMimeType,
		byte[] banner,
		String bannerMimeType) {

	public boolean hasPhoto() {
		return photo != null;
	}

	public boolean hasBanner() {
		return banner != null;
	}

	public List<ProfileSection> missingSections() {
		List<ProfileSection> missing = new ArrayList<>();
		addIf(missing, !hasPhoto(), ProfileSection.PHOTO);
		addIf(missing, !hasBanner(), ProfileSection.BANNER);
		addIf(missing, profile.headline() == null, ProfileSection.HEADLINE);
		addIf(missing, profile.about() == null, ProfileSection.ABOUT);
		addIf(missing, profile.customUrl() == null, ProfileSection.CUSTOM_URL);
		addIf(missing, profile.experiences().isEmpty(), ProfileSection.EXPERIENCE);
		addIf(missing, profile.educations().isEmpty(), ProfileSection.EDUCATION);
		addIf(missing, profile.skills().isEmpty(), ProfileSection.SKILLS);
		addIf(missing, profile.certifications().isEmpty(), ProfileSection.CERTIFICATIONS);
		addIf(missing, profile.languages().isEmpty(), ProfileSection.LANGUAGES);
		addIf(missing, profile.projects().isEmpty(), ProfileSection.PROJECTS);
		addIf(missing, profile.publications().isEmpty(), ProfileSection.PUBLICATIONS);
		addIf(missing, profile.recommendations().isEmpty(), ProfileSection.RECOMMENDATIONS);
		addIf(missing, profile.featured().isEmpty(), ProfileSection.FEATURED);
		addIf(missing, profile.recentActivity().isEmpty(), ProfileSection.ACTIVITY);
		return List.copyOf(missing);
	}

	private static void addIf(List<ProfileSection> target, boolean condition, ProfileSection section) {
		if (condition) {
			target.add(section);
		}
	}
}
