package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import com.diegohaefliger.atsresumeoptimizer.careerprofile.CareerProfile;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInEntry;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInProfile;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSnapshot;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

final class LinkedInProfileChecks {

	static final int HEADLINE_MAX_LENGTH = 220;
	static final int HEADLINE_MIN_LENGTH = 40;
	static final int ABOUT_MIN_LENGTH = 300;
	static final int SKILLS_MIN_COUNT = 5;
	static final int PINNED_SKILLS = 3;

	private static final int MIN_TOKEN_LENGTH = 3;
	private static final Pattern DIACRITICS = Pattern.compile("\\p{M}+");
	private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9+#.]+");
	private static final Set<String> STOP_WORDS = Set.of("de", "da", "do", "das", "dos", "para", "com", "and", "the", "for");

	private LinkedInProfileChecks() {
	}

	static List<String> run(LinkedInSnapshot snapshot, CareerProfile career) {
		LinkedInProfile profile = snapshot.profile();
		List<String> checks = new ArrayList<>();
		headline(checks, profile, career);
		about(checks, profile);
		skills(checks, profile, career);
		experiences(checks, profile);
		customUrl(checks, profile);
		return checks;
	}

	private static void headline(List<String> checks, LinkedInProfile profile, CareerProfile career) {
		String headline = profile.headline();
		if (headline == null) {
			checks.add("Título (headline) ausente.");
			return;
		}
		if (headline.length() < HEADLINE_MIN_LENGTH) {
			checks.add("Título (headline) curto: " + headline.length() + " caracteres, abaixo de " + HEADLINE_MIN_LENGTH + ".");
		}
		if (headline.length() > HEADLINE_MAX_LENGTH) {
			checks.add("Título (headline) acima do limite de " + HEADLINE_MAX_LENGTH + " caracteres.");
		}
		missingTerms(checks, "o cargo-alvo", career.targetJobTitle(), headline);
		missingTerms(checks, "a senioridade alvo", career.targetSeniority(), headline);
	}

	private static void missingTerms(List<String> checks, String label, String wanted, String headline) {
		List<String> tokens = tokens(wanted);
		if (tokens.isEmpty()) {
			return;
		}
		Set<String> present = Set.copyOf(tokens(headline));
		List<String> missing = tokens.stream().filter(token -> !present.contains(token)).toList();
		if (missing.size() == tokens.size()) {
			checks.add("Título (headline) não menciona " + label + " (\"" + wanted + "\").");
		}
	}

	private static void about(List<String> checks, LinkedInProfile profile) {
		if (profile.about() == null) {
			checks.add("Seção Sobre ausente.");
		} else if (profile.about().length() < ABOUT_MIN_LENGTH) {
			checks.add("Seção Sobre curta: " + profile.about().length() + " caracteres, abaixo de " + ABOUT_MIN_LENGTH + ".");
		}
	}

	private static void skills(List<String> checks, LinkedInProfile profile, CareerProfile career) {
		List<String> skills = profile.skills();
		if (skills.size() < SKILLS_MIN_COUNT) {
			checks.add("Poucas competências: " + skills.size() + ", abaixo de " + SKILLS_MIN_COUNT + ".");
		}
		List<String> strengths = career.strengths().stream().map(LinkedInProfileChecks::normalized).toList();
		if (skills.isEmpty() || strengths.isEmpty()) {
			return;
		}
		List<String> pinned = skills.stream().limit(PINNED_SKILLS).map(LinkedInProfileChecks::normalized).toList();
		if (pinned.stream().noneMatch(strengths::contains)) {
			checks.add("Nenhuma das " + PINNED_SKILLS + " competências fixadas está entre os pontos fortes do perfil de carreira.");
		}
		List<String> listed = skills.stream().map(LinkedInProfileChecks::normalized).toList();
		List<String> absent = career.strengths().stream().filter(strength -> !listed.contains(normalized(strength))).toList();
		if (!absent.isEmpty()) {
			checks.add("Pontos fortes do perfil de carreira ausentes nas competências: " + String.join(", ", absent) + ".");
		}
	}

	private static void experiences(List<String> checks, LinkedInProfile profile) {
		List<LinkedInEntry> experiences = profile.experiences();
		if (experiences.isEmpty()) {
			checks.add("Nenhuma experiência no perfil.");
			return;
		}
		long withoutDescription = experiences.stream().filter(entry -> entry.description() == null).count();
		if (withoutDescription > 0) {
			checks.add(withoutDescription + " de " + experiences.size() + " experiências sem descrição.");
		}
	}

	private static void customUrl(List<String> checks, LinkedInProfile profile) {
		if (profile.customUrl() == null) {
			checks.add("URL personalizada não definida.");
		}
	}

	private static List<String> tokens(String text) {
		if (text == null) {
			return List.of();
		}
		return Arrays.stream(normalized(text).split("\\s+"))
				.filter(token -> token.length() >= MIN_TOKEN_LENGTH && !STOP_WORDS.contains(token))
				.distinct()
				.toList();
	}

	private static String normalized(String text) {
		String plain = DIACRITICS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
		return NON_ALPHANUMERIC.matcher(plain.toLowerCase(Locale.ROOT)).replaceAll(" ").strip();
	}
}
