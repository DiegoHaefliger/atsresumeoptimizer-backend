package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JobDescriptionFormatterTest {

	@Test
	void turnsKnownSectionNamesIntoBoldTopicsSeparatedByBlankLines() {
		String text = JobDescriptionFormatter.format("Somos uma fintech.\nResponsabilidades:\n• Desenvolver APIs\n• Revisar código\nRequisitos\n- Java 21\n- Spring Boot");

		assertThat(text).isEqualTo("""
				Somos uma fintech.

				**Responsabilidades**
				- Desenvolver APIs
				- Revisar código

				**Requisitos**
				- Java 21
				- Spring Boot""");
	}

	@Test
	void splitsAHeadingFollowedByItemsOnTheSameLine() {
		String text = JobDescriptionFormatter.format("Requisitos: Java; Spring Boot; Docker\n\nBenefícios: Vale-refeição, Plano de saúde");

		assertThat(text).isEqualTo("""
				**Requisitos**
				- Java
				- Spring Boot
				- Docker

				**Benefícios**
				- Vale-refeição
				- Plano de saúde""");
	}

	@Test
	void turnsLinesUnderATopicIntoABulletList() {
		String text = JobDescriptionFormatter.format("Atividades:\nDesenvolver serviços em Java;\nAtuar com mensageria;\nCuidar da observabilidade.");

		assertThat(text).isEqualTo("**Atividades**\n- Desenvolver serviços em Java;\n- Atuar com mensageria;\n- Cuidar da observabilidade.");
	}

	@Test
	void keepsProseWithoutTopicsAsIs() {
		String prose = "Buscamos uma pessoa para atuar no time de pagamentos.\n\nO trabalho é remoto.";

		assertThat(JobDescriptionFormatter.format(prose)).isEqualTo(prose);
	}

	@Test
	void doesNotTreatALongLineStartingWithAKnownWordAsATopic() {
		String line = "Requisitos de segurança são levados muito a sério por toda a equipe de engenharia aqui.";

		assertThat(JobDescriptionFormatter.format(line)).isEqualTo(line);
	}

	@Test
	void returnsEmptyForBlankText() {
		assertThat(JobDescriptionFormatter.format("  ")).isEmpty();
		assertThat(JobDescriptionFormatter.format(null)).isEmpty();
	}
}
