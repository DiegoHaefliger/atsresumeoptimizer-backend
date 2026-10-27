package com.diegohaefliger.atsresumeoptimizer.careerprofile.domain;

import java.util.List;

public record CareerProfile(
		String areaOfExpertise,
		String currentJobTitle,
		String currentSeniority,
		String targetJobTitle,
		String targetSeniority,
		String targetTimeframe,
		List<String> targetIndustries,
		List<String> targetCompanies,
		String location,
		List<String> languages,
		List<String> strengths,
		String differentiators,
		ProfileTone tone,
		String profileLanguage,
		String notes) {

	public static final String DEFAULT_PROFILE_LANGUAGE = "pt-BR";

	public CareerProfile {
		areaOfExpertise = stripped(areaOfExpertise);
		currentJobTitle = stripped(currentJobTitle);
		currentSeniority = stripped(currentSeniority);
		targetJobTitle = stripped(targetJobTitle);
		targetSeniority = stripped(targetSeniority);
		targetTimeframe = stripped(targetTimeframe);
		location = stripped(location);
		differentiators = stripped(differentiators);
		notes = stripped(notes);
		targetIndustries = cleaned(targetIndustries);
		targetCompanies = cleaned(targetCompanies);
		languages = cleaned(languages);
		strengths = cleaned(strengths);
		tone = tone == null ? ProfileTone.TECHNICAL : tone;
		profileLanguage = profileLanguage == null || profileLanguage.isBlank() ? DEFAULT_PROFILE_LANGUAGE : profileLanguage.strip();
		if (areaOfExpertise == null) {
			throw new InvalidCareerProfileException("Informe a área de atuação.");
		}
		if (targetJobTitle == null) {
			throw new InvalidCareerProfileException("Informe o próximo cargo que você deseja alcançar.");
		}
	}

	private static String stripped(String value) {
		return value == null || value.isBlank() ? null : value.strip();
	}

	private static List<String> cleaned(List<String> values) {
		if (values == null) {
			return List.of();
		}
		return values.stream().filter(value -> value != null && !value.isBlank()).map(String::strip).distinct().toList();
	}
}
