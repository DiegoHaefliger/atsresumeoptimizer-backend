package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.careerprofile.CareerProfile;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInEntry;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInProfile;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSnapshot;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.SnapshotSource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LinkedInProfileChecksTest {

	private static CareerProfile career(String target, String seniority, List<String> strengths) {
		return new CareerProfile("Engenharia", "Dev", "Pleno", target, seniority, null, List.of(), List.of(), null,
				List.of(), strengths, null, null, null, null);
	}

	private static LinkedInSnapshot snapshot(String headline, String about, List<LinkedInEntry> experiences,
			List<String> skills, String customUrl) {
		LinkedInProfile profile = new LinkedInProfile(null, "Ana", headline, null, about, customUrl, null, null,
				experiences, null, null, null, null, skills, null, null, null, null);
		return new LinkedInSnapshot(UUID.randomUUID(), Instant.now(), SnapshotSource.EXTENSION, profile, null, null,
				null, null);
	}

	@Test
	void flagsMissingHeadlineAndAbout() {
		List<String> checks = LinkedInProfileChecks.run(snapshot(null, null, null, null, null),
				career("Tech Lead", null, List.of()));

		assertThat(checks).contains("Título (headline) ausente.", "Seção Sobre ausente.",
				"Nenhuma experiência no perfil.", "URL personalizada não definida.");
	}

	@Test
	void flagsHeadlineWithoutTargetTitleIgnoringAccents() {
		String headline = "Desenvolvedora Java com foco em APIs REST e microsserviços na nuvem";

		List<String> without = LinkedInProfileChecks.run(snapshot(headline, null, null, null, "ana"),
				career("Tech Lead", "Sênior", List.of()));
		List<String> with = LinkedInProfileChecks.run(
				snapshot(headline + " | Senior Tech Lead", null, null, null, "ana"),
				career("Tech Lead", "Sênior", List.of()));

		assertThat(without).anyMatch(check -> check.contains("não menciona o cargo-alvo"))
				.anyMatch(check -> check.contains("não menciona a senioridade alvo"));
		assertThat(with).noneMatch(check -> check.contains("não menciona"));
	}

	@Test
	void flagsShortHeadlineAndLongHeadline() {
		assertThat(LinkedInProfileChecks.run(snapshot("Dev", null, null, null, "a"), career("Tech Lead", null, List.of())))
				.anyMatch(check -> check.startsWith("Título (headline) curto"));
		assertThat(LinkedInProfileChecks.run(snapshot("x".repeat(221), null, null, null, "a"),
				career("Tech Lead", null, List.of()))).anyMatch(check -> check.contains("acima do limite"));
	}

	@Test
	void flagsSkillGapsAgainstStrengths() {
		List<String> checks = LinkedInProfileChecks.run(
				snapshot(null, null, null, List.of("Excel", "Word", "Outlook", "Java"), "a"),
				career("Tech Lead", null, List.of("java", "Spring")));

		assertThat(checks).anyMatch(check -> check.startsWith("Poucas competências"))
				.anyMatch(check -> check.contains("fixadas"))
				.contains("Pontos fortes do perfil de carreira ausentes nas competências: Spring.");
	}

	@Test
	void countsExperiencesWithoutDescription() {
		List<LinkedInEntry> experiences = List.of(new LinkedInEntry("Dev", "Acme", null, null, "fiz X"),
				new LinkedInEntry("Dev", "Beta", null, null, null));

		assertThat(LinkedInProfileChecks.run(snapshot(null, null, experiences, null, "a"),
				career("Tech Lead", null, List.of()))).contains("1 de 2 experiências sem descrição.");
	}

	@Test
	void cleanProfileProducesNoChecks() {
		String about = "x".repeat(LinkedInProfileChecks.ABOUT_MIN_LENGTH);
		String headline = "Tech Lead Sênior | Java, Spring e arquitetura de microsserviços";
		List<LinkedInEntry> experiences = List.of(new LinkedInEntry("Dev", "Acme", null, null, "fiz X"));

		assertThat(LinkedInProfileChecks.run(
				snapshot(headline, about, experiences, List.of("Java", "Spring", "SQL", "Git", "Docker"), "ana"),
				career("Tech Lead", "Sênior", List.of("Java", "Spring")))).isEmpty();
	}
}
