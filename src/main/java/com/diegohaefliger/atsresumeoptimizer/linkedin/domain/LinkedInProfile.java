package com.diegohaefliger.atsresumeoptimizer.linkedin.domain;

import java.util.List;

public record LinkedInProfile(
		String profileUrl,
		String fullName,
		String headline,
		String location,
		String about,
		String customUrl,
		String connections,
		String followers,
		List<LinkedInEntry> experiences,
		List<LinkedInEntry> educations,
		List<LinkedInEntry> certifications,
		List<LinkedInEntry> projects,
		List<LinkedInEntry> publications,
		List<String> skills,
		List<String> languages,
		List<String> recommendations,
		List<String> featured,
		List<String> recentActivity) {

	public LinkedInProfile {
		profileUrl = LinkedInEntry.stripped(profileUrl);
		fullName = LinkedInEntry.stripped(fullName);
		headline = LinkedInEntry.stripped(headline);
		location = LinkedInEntry.stripped(location);
		about = LinkedInEntry.stripped(about);
		customUrl = LinkedInEntry.stripped(customUrl);
		connections = LinkedInEntry.stripped(connections);
		followers = LinkedInEntry.stripped(followers);
		experiences = entries(experiences);
		educations = entries(educations);
		certifications = entries(certifications);
		projects = entries(projects);
		publications = entries(publications);
		skills = texts(skills);
		languages = texts(languages);
		recommendations = texts(recommendations);
		featured = texts(featured);
		recentActivity = texts(recentActivity);
	}

	public boolean lacksContent() {
		return headline == null && about == null && experiences.isEmpty() && educations.isEmpty() && skills.isEmpty();
	}

	private static List<LinkedInEntry> entries(List<LinkedInEntry> values) {
		if (values == null) {
			return List.of();
		}
		return values.stream()
				.filter(entry -> entry != null && (entry.title() != null || entry.organization() != null))
				.toList();
	}

	private static List<String> texts(List<String> values) {
		if (values == null) {
			return List.of();
		}
		return values.stream().filter(value -> value != null && !value.isBlank()).map(String::strip).distinct().toList();
	}
}
