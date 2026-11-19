package com.diegohaefliger.atsresumeoptimizer.ai;

import java.util.List;

public record LinkedInProfileDraft(
		String fullName,
		String headline,
		String location,
		String about,
		List<LinkedInProfileEntryDraft> experiences,
		List<LinkedInProfileEntryDraft> educations,
		List<LinkedInProfileEntryDraft> certifications,
		List<LinkedInProfileEntryDraft> projects,
		List<LinkedInProfileEntryDraft> publications,
		List<String> skills,
		List<String> languages) {

	public LinkedInProfileDraft {
		experiences = experiences == null ? List.of() : List.copyOf(experiences);
		educations = educations == null ? List.of() : List.copyOf(educations);
		certifications = certifications == null ? List.of() : List.copyOf(certifications);
		projects = projects == null ? List.of() : List.copyOf(projects);
		publications = publications == null ? List.of() : List.copyOf(publications);
		skills = skills == null ? List.of() : List.copyOf(skills);
		languages = languages == null ? List.of() : List.copyOf(languages);
	}
}
