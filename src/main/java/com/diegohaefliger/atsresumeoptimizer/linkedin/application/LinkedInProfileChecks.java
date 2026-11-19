package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import com.diegohaefliger.atsresumeoptimizer.careerprofile.CareerProfile;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInEntry;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInProfile;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSnapshot;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.ProfileCheck;
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

	static List<ProfileCheck> run(LinkedInSnapshot snapshot, CareerProfile career) {
		LinkedInProfile profile = snapshot.profile();
		List<ProfileCheck> checks = new ArrayList<>();
		headline(checks, profile, career);
		about(checks, profile);
		skills(checks, profile, career);
		experiences(checks, profile);
		checks.add(profile.customUrl() == null
				? ProfileCheck.failed("URL personalizada definida", "O endereço do perfil ainda é o padrão do LinkedIn.")
				: ProfileCheck.ok("URL personalizada definida"));
		return checks;
	}

	private static void headline(List<ProfileCheck> checks, LinkedInProfile profile, CareerProfile career) {
		String label = "Título com tamanho adequado";
		String headline = profile.headline();
		if (headline == null) {
			checks.add(ProfileCheck.failed(label, "O perfil não tem título (headline)."));
			return;
		}
		if (headline.length() < HEADLINE_MIN_LENGTH) {
			checks.add(ProfileCheck.failed(label, "Tem " + headline.length() + " caracteres, o ideal é a partir de "
					+ HEADLINE_MIN_LENGTH + "."));
		} else if (headline.length() > HEADLINE_MAX_LENGTH) {
			checks.add(ProfileCheck.failed(label, "Passa do limite de " + HEADLINE_MAX_LENGTH + " caracteres."));
		} else {
			checks.add(ProfileCheck.ok(label));
		}
		mentions(checks, "Título cita o cargo-alvo", career.targetJobTitle(), headline);
		mentions(checks, "Título cita a senioridade alvo", career.targetSeniority(), headline);
	}

	private static void mentions(List<ProfileCheck> checks, String label, String wanted, String headline) {
		List<String> tokens = tokens(wanted);
		if (tokens.isEmpty()) {
			return;
		}
		Set<String> present = Set.copyOf(tokens(headline));
		boolean found = tokens.stream().anyMatch(present::contains);
		checks.add(found ? ProfileCheck.ok(label) : ProfileCheck.failed(label, "\"" + wanted + "\" não aparece no título."));
	}

	private static void about(List<ProfileCheck> checks, LinkedInProfile profile) {
		String label = "Sobre com texto suficiente";
		if (profile.about() == null) {
			checks.add(ProfileCheck.failed(label, "O perfil não tem a seção Sobre."));
		} else if (profile.about().length() < ABOUT_MIN_LENGTH) {
			checks.add(ProfileCheck.failed(label, "Tem " + profile.about().length() + " caracteres, o ideal é a partir de "
					+ ABOUT_MIN_LENGTH + "."));
		} else {
			checks.add(ProfileCheck.ok(label));
		}
	}

	private static void skills(List<ProfileCheck> checks, LinkedInProfile profile, CareerProfile career) {
		List<String> skills = profile.skills();
		String label = "Competências suficientes";
		checks.add(skills.size() < SKILLS_MIN_COUNT
				? ProfileCheck.failed(label, "Tem " + skills.size() + ", o ideal é a partir de " + SKILLS_MIN_COUNT + ".")
				: ProfileCheck.ok(label));
		if (skills.isEmpty() || career.strengths().isEmpty()) {
			return;
		}
		List<String> strengths = career.strengths().stream().map(LinkedInProfileChecks::normalized).toList();
		List<String> pinned = skills.stream().limit(PINNED_SKILLS).map(LinkedInProfileChecks::normalized).toList();
		String pinnedLabel = "Competências fixadas ligadas aos seus pontos fortes";
		checks.add(pinned.stream().anyMatch(strengths::contains)
				? ProfileCheck.ok(pinnedLabel)
				: ProfileCheck.failed(pinnedLabel, "Nenhuma das " + PINNED_SKILLS
						+ " primeiras competências é um dos pontos fortes do seu perfil de carreira."));
		List<String> listed = skills.stream().map(LinkedInProfileChecks::normalized).toList();
		List<String> absent = career.strengths().stream().filter(strength -> !listed.contains(normalized(strength))).toList();
		String coverageLabel = "Pontos fortes listados nas competências";
		checks.add(absent.isEmpty()
				? ProfileCheck.ok(coverageLabel)
				: ProfileCheck.failed(coverageLabel, "Faltam: " + String.join(", ", absent) + "."));
	}

	private static void experiences(List<ProfileCheck> checks, LinkedInProfile profile) {
		String label = "Experiências com descrição";
		List<LinkedInEntry> experiences = profile.experiences();
		if (experiences.isEmpty()) {
			checks.add(ProfileCheck.failed(label, "O perfil não tem experiências."));
			return;
		}
		long withoutDescription = experiences.stream().filter(entry -> entry.description() == null).count();
		checks.add(withoutDescription == 0
				? ProfileCheck.ok(label)
				: ProfileCheck.failed(label, withoutDescription + " de " + experiences.size() + " estão sem descrição."));
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
