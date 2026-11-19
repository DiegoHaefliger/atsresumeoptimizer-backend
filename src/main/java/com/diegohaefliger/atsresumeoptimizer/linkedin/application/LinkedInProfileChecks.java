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
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class LinkedInProfileChecks {

	static final int HEADLINE_MAX_LENGTH = 220;
	static final int HEADLINE_MIN_LENGTH = 40;
	static final int ABOUT_MIN_LENGTH = 300;
	static final int SKILLS_MIN_COUNT = 5;

	private static final String HEADLINE = "Título (headline)";
	private static final String ABOUT = "Sobre";
	private static final String SKILLS = "Competências";
	private static final String EXPERIENCES = "Experiência";
	private static final String CUSTOM_URL = "URL personalizada";
	private static final String STRENGTHS_PLACES = "Sobre, Experiência ou Competências";
	private static final String NOT_FILLED = "Não preenchido";
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
		customUrl(checks, profile);
		return checks;
	}

	private static void headline(List<ProfileCheck> checks, LinkedInProfile profile, CareerProfile career) {
		String label = "Título com tamanho adequado";
		String headline = profile.headline();
		if (headline == null) {
			checks.add(ProfileCheck.failed(label, HEADLINE, NOT_FILLED, "Escreva um título de " + HEADLINE_MIN_LENGTH + " a "
					+ HEADLINE_MAX_LENGTH + " caracteres com o cargo, a especialidade e as principais tecnologias."));
			return;
		}
		String current = quoted(headline) + " (" + headline.length() + " caracteres)";
		if (headline.length() < HEADLINE_MIN_LENGTH) {
			checks.add(ProfileCheck.failed(label, HEADLINE, current, "Amplie para pelo menos " + HEADLINE_MIN_LENGTH
					+ " caracteres, somando especialidades e tecnologias que o recrutador busca."));
		} else if (headline.length() > HEADLINE_MAX_LENGTH) {
			checks.add(ProfileCheck.failed(label, HEADLINE, current, "Reduza para no máximo " + HEADLINE_MAX_LENGTH
					+ " caracteres, mantendo o cargo e as palavras-chave principais."));
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
		checks.add(found
				? ProfileCheck.ok(label)
				: ProfileCheck.failed(label, HEADLINE, quoted(headline), "Inclua \"" + wanted + "\" no título."));
	}

	private static void about(List<ProfileCheck> checks, LinkedInProfile profile) {
		String label = "Sobre com texto suficiente";
		String action = "Escreva pelo menos " + ABOUT_MIN_LENGTH
				+ " caracteres: quem você é, resultados com números, especialidades e o que busca.";
		if (profile.about() == null) {
			checks.add(ProfileCheck.failed(label, ABOUT, NOT_FILLED, action));
		} else if (profile.about().length() < ABOUT_MIN_LENGTH) {
			checks.add(ProfileCheck.failed(label, ABOUT, "Tem " + profile.about().length() + " caracteres", action));
		} else {
			checks.add(ProfileCheck.ok(label));
		}
	}

	private static void skills(List<ProfileCheck> checks, LinkedInProfile profile, CareerProfile career) {
		List<String> skills = profile.skills();
		String label = "Competências suficientes";
		if (skills.size() < SKILLS_MIN_COUNT) {
			String current = skills.isEmpty() ? "Nenhuma competência" : "Tem " + skills.size() + ": " + String.join(", ", skills);
			checks.add(ProfileCheck.failed(label, SKILLS, current, "Adicione pelo menos " + (SKILLS_MIN_COUNT - skills.size())
					+ " competência(s) ligadas a " + career.targetJobTitle() + ", e deixe as 3 primeiras como as mais importantes."));
		} else {
			checks.add(ProfileCheck.ok(label));
		}
		if (career.strengths().isEmpty()) {
			return;
		}
		Set<String> present = Set.copyOf(tokens(profileText(profile)));
		List<String> absent = career.strengths().stream()
				.filter(strength -> !tokens(strength).isEmpty() && !present.containsAll(tokens(strength)))
				.toList();
		String coverageLabel = "Pontos fortes aparecem no perfil";
		checks.add(absent.isEmpty()
				? ProfileCheck.ok(coverageLabel)
				: ProfileCheck.failed(coverageLabel, STRENGTHS_PLACES, "Não aparecem: " + String.join(", ", absent),
						"Inclua esses termos no Sobre, nas experiências ou nas competências, só se forem verdade."));
	}

	private static String profileText(LinkedInProfile profile) {
		List<String> parts = new ArrayList<>(profile.skills());
		parts.add(profile.headline());
		parts.add(profile.about());
		for (LinkedInEntry entry : profile.experiences()) {
			parts.add(entry.title());
			parts.add(entry.description());
		}
		return parts.stream().filter(Objects::nonNull).collect(Collectors.joining(" "));
	}

	private static void experiences(List<ProfileCheck> checks, LinkedInProfile profile) {
		String label = "Experiências com descrição";
		List<LinkedInEntry> experiences = profile.experiences();
		if (experiences.isEmpty()) {
			checks.add(ProfileCheck.failed(label, EXPERIENCES, "Nenhuma experiência cadastrada",
					"Cadastre suas experiências com resultados e tecnologias."));
			return;
		}
		List<String> without = experiences.stream().filter(entry -> entry.description() == null)
				.map(LinkedInProfileChecks::experienceName).toList();
		checks.add(without.isEmpty()
				? ProfileCheck.ok(label)
				: ProfileCheck.failed(label, EXPERIENCES, "Sem descrição: " + String.join("; ", without),
						"Descreva cada uma com resultados mensuráveis e as tecnologias usadas."));
	}

	private static String experienceName(LinkedInEntry entry) {
		return Stream.of(entry.organization(), entry.title()).filter(Objects::nonNull).collect(Collectors.joining(" - "));
	}

	private static void customUrl(List<ProfileCheck> checks, LinkedInProfile profile) {
		String label = "URL personalizada definida";
		checks.add(profile.customUrl() != null
				? ProfileCheck.ok(label)
				: ProfileCheck.failed(label, CUSTOM_URL, profile.profileUrl() == null ? "Endereço padrão do LinkedIn" : profile.profileUrl(),
						"Defina um endereço curto com o seu nome em \"Editar perfil público e URL\"."));
	}

	private static String quoted(String text) {
		return "\"" + text + "\"";
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
