package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import com.diegohaefliger.atsresumeoptimizer.Sha256;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInEntry;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInProfile;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSnapshot;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.ProfileSection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

final class LinkedInContentFingerprint {

	private static final String FIELD_SEPARATOR = "\u001f";
	private static final String ENTRY_SEPARATOR = "\u001e";

	private LinkedInContentFingerprint() {
	}

	static Map<ProfileSection, String> of(LinkedInSnapshot snapshot) {
		return of(snapshot.profile(), snapshot.photo(), snapshot.banner());
	}

	static Map<ProfileSection, String> of(LinkedInProfile profile, byte[] photo, byte[] banner) {
		Map<ProfileSection, String> hashes = new EnumMap<>(ProfileSection.class);
		put(hashes, ProfileSection.PHOTO, photo == null ? "" : Sha256.hex(photo));
		put(hashes, ProfileSection.BANNER, banner == null ? "" : Sha256.hex(banner));
		put(hashes, ProfileSection.HEADLINE, join(profile.headline(), profile.location()));
		put(hashes, ProfileSection.ABOUT, join(profile.about()));
		put(hashes, ProfileSection.CUSTOM_URL, join(profile.customUrl()));
		put(hashes, ProfileSection.EXPERIENCE, entries(profile.experiences()));
		put(hashes, ProfileSection.EDUCATION, entries(profile.educations()));
		put(hashes, ProfileSection.SKILLS, texts(profile.skills()));
		put(hashes, ProfileSection.CERTIFICATIONS, entries(profile.certifications()));
		put(hashes, ProfileSection.LANGUAGES, texts(profile.languages()));
		put(hashes, ProfileSection.PROJECTS, entries(profile.projects()));
		put(hashes, ProfileSection.PUBLICATIONS, entries(profile.publications()));
		put(hashes, ProfileSection.RECOMMENDATIONS, texts(profile.recommendations()));
		put(hashes, ProfileSection.FEATURED, texts(profile.featured()));
		put(hashes, ProfileSection.ACTIVITY, texts(profile.recentActivity()));
		return hashes;
	}

	static List<ProfileSection> changed(Map<ProfileSection, String> before, Map<ProfileSection, String> after) {
		List<ProfileSection> changed = new ArrayList<>();
		for (ProfileSection section : ProfileSection.values()) {
			if (!Objects.equals(before.get(section), after.get(section))) {
				changed.add(section);
			}
		}
		return List.copyOf(changed);
	}

	private static void put(Map<ProfileSection, String> hashes, ProfileSection section, String content) {
		hashes.put(section, content.isEmpty() ? "" : Sha256.hex(content));
	}

	private static String join(String... values) {
		if (Arrays.stream(values).allMatch(Objects::isNull)) {
			return "";
		}
		return Arrays.stream(values).map(value -> value == null ? "" : value).collect(Collectors.joining(FIELD_SEPARATOR));
	}

	private static String texts(List<String> values) {
		return String.join(ENTRY_SEPARATOR, values);
	}

	private static String entries(List<LinkedInEntry> entries) {
		return entries.stream()
				.map(entry -> join(entry.title(), entry.organization(), entry.period(), entry.location(), entry.description()))
				.collect(Collectors.joining(ENTRY_SEPARATOR));
	}
}
