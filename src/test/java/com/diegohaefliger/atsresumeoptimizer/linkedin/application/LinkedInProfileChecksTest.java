package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.careerprofile.CareerProfile;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInEntry;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInProfile;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSnapshot;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.ProfileCheck;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.SnapshotSource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LinkedInProfileChecksTest {

	private static CareerProfile career(String seniority, List<String> strengths) {
		return new CareerProfile("Engenharia", "Dev", "Pleno", "Tech Lead", seniority, null, List.of(), List.of(), null,
				List.of(), strengths, null, null, null, null);
	}

	private static LinkedInSnapshot snapshot(String headline, String about, List<LinkedInEntry> experiences,
			List<String> skills, String customUrl) {
		LinkedInProfile profile = new LinkedInProfile(null, "Ana", headline, null, about, customUrl, null, null,
				experiences, null, null, null, null, skills, null, null, null, null);
		return new LinkedInSnapshot(UUID.randomUUID(), Instant.now(), SnapshotSource.EXTENSION, profile, null, null,
				null, null);
	}

	private static ProfileCheck check(List<ProfileCheck> checks, String label) {
		return checks.stream().filter(candidate -> candidate.label().equals(label)).findFirst().orElseThrow();
	}

	@Test
	void flagsMissingHeadlineAboutExperiencesAndUrl() {
		List<ProfileCheck> checks = LinkedInProfileChecks.run(snapshot(null, null, null, null, null), career(null, List.of()));

		assertThat(check(checks, "Título com tamanho adequado").detail()).isEqualTo("O perfil não tem título (headline).");
		assertThat(check(checks, "Sobre com texto suficiente").passed()).isFalse();
		assertThat(check(checks, "Experiências com descrição").passed()).isFalse();
		assertThat(check(checks, "URL personalizada definida").passed()).isFalse();
		assertThat(checks).noneMatch(candidate -> candidate.label().startsWith("Título cita"));
	}

	@Test
	void matchesTargetTitleAndSeniorityIgnoringAccents() {
		String headline = "Desenvolvedora Java com foco em APIs REST e microsserviços na nuvem";

		List<ProfileCheck> without = LinkedInProfileChecks.run(snapshot(headline, null, null, null, "ana"),
				career("Sênior", List.of()));
		List<ProfileCheck> with = LinkedInProfileChecks.run(snapshot(headline + " | Senior Tech Lead", null, null, null, "ana"),
				career("Sênior", List.of()));

		assertThat(check(without, "Título cita o cargo-alvo").passed()).isFalse();
		assertThat(check(without, "Título cita a senioridade alvo").passed()).isFalse();
		assertThat(check(with, "Título cita o cargo-alvo").passed()).isTrue();
		assertThat(check(with, "Título cita a senioridade alvo").passed()).isTrue();
	}

	@Test
	void checksHeadlineLength() {
		assertThat(check(LinkedInProfileChecks.run(snapshot("Dev", null, null, null, "a"), career(null, List.of())),
				"Título com tamanho adequado").detail()).contains("3 caracteres");
		assertThat(check(LinkedInProfileChecks.run(snapshot("x".repeat(221), null, null, null, "a"), career(null, List.of())),
				"Título com tamanho adequado").detail()).contains("limite");
	}

	@Test
	void checksSkillsAgainstStrengths() {
		List<ProfileCheck> checks = LinkedInProfileChecks.run(
				snapshot(null, null, null, List.of("Excel", "Word", "Outlook", "Java"), "a"),
				career(null, List.of("java", "Spring")));

		assertThat(check(checks, "Competências suficientes").detail()).isEqualTo("Tem 4, o ideal é a partir de 5.");
		assertThat(check(checks, "Competências fixadas ligadas aos seus pontos fortes").passed()).isFalse();
		assertThat(check(checks, "Pontos fortes listados nas competências").detail()).isEqualTo("Faltam: Spring.");
	}

	@Test
	void skipsStrengthChecksWhenNoneAreRegistered() {
		List<ProfileCheck> checks = LinkedInProfileChecks.run(snapshot(null, null, null, List.of("Java"), "a"),
				career(null, List.of()));

		assertThat(checks).noneMatch(candidate -> candidate.label().contains("pontos fortes"));
	}

	@Test
	void countsExperiencesWithoutDescription() {
		List<LinkedInEntry> experiences = List.of(new LinkedInEntry("Dev", "Acme", null, null, "fiz X"),
				new LinkedInEntry("Dev", "Beta", null, null, null));

		assertThat(check(LinkedInProfileChecks.run(snapshot(null, null, experiences, null, "a"), career(null, List.of())),
				"Experiências com descrição").detail()).isEqualTo("1 de 2 estão sem descrição.");
	}

	@Test
	void cleanProfilePassesEveryCheck() {
		String about = "x".repeat(LinkedInProfileChecks.ABOUT_MIN_LENGTH);
		String headline = "Tech Lead Sênior | Java, Spring e arquitetura de microsserviços";
		List<LinkedInEntry> experiences = List.of(new LinkedInEntry("Dev", "Acme", null, null, "fiz X"));

		List<ProfileCheck> checks = LinkedInProfileChecks.run(
				snapshot(headline, about, experiences, List.of("Java", "Spring", "SQL", "Git", "Docker"), "ana"),
				career("Sênior", List.of("Java", "Spring")));

		assertThat(checks).isNotEmpty().allMatch(ProfileCheck::passed);
	}

	@Test
	void promptListsOnlyFailedChecksAndTellsAiNotToRepeatThem() {
		String text = LinkedInPromptText.withChecks("PERFIL", List.of(ProfileCheck.ok("A"), ProfileCheck.failed("B", "falta X")));

		assertThat(text).contains("- B: falta X").doesNotContain("- A").contains("NÃO as repita");
		assertThat(LinkedInPromptText.withChecks("PERFIL", List.of(ProfileCheck.ok("A")))).isEqualTo("PERFIL");
	}
}
