package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ContactInfo;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.NormalizedDocument;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingResult;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingSignals;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import com.diegohaefliger.atsresumeoptimizer.rewrite.ResumeContact;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ParsedResumeImporterTest {

	private static final String RAW_TEXT = """
			Ana Silva
			Desenvolvedora Backend
			ana@email.com | (55) 99999-0000
			Resumo
			Backend com 6 anos de Java.
			Foco em APIs.
			Experiência Profissional
			• Criou APIs REST em Java
			- Reduziu custo de nuvem em 20%
			""";

	@Test
	void keepsTheTextAsWrittenAndGuessesWhatEachSectionIs() {
		StructuredResume content = ParsedResumeImporter.content(parsingResult());

		assertThat(content.name()).isEqualTo("Ana Silva");
		assertThat(content.headline()).isEqualTo("Desenvolvedora Backend");
		assertThat(content.sections()).extracting(ResumeSection::semanticType, ResumeSection::kind).containsExactly(
				org.assertj.core.groups.Tuple.tuple(ResumeSectionSemanticType.SUMMARY, ResumeSectionKind.PARAGRAPH),
				org.assertj.core.groups.Tuple.tuple(ResumeSectionSemanticType.EXPERIENCE, ResumeSectionKind.RICH_LINES));
		assertThat(content.sections().get(0).paragraph()).isEqualTo("Backend com 6 anos de Java. Foco em APIs.");
		assertThat(content.sections().get(1).richLines()).containsExactly(
				List.of(new TextSpan("Criou APIs REST em Java", false)),
				List.of(new TextSpan("Reduziu custo de nuvem em 20%", false)));
	}

	@Test
	void joinsALineThatThePdfWrappedMidSentenceButKeepsSeparateItemsApart() {
		Section experience = new Section("Experiência Profissional", true, """
				Porto | São Paulo–SP (remoto)
				Migração de 5 projetos legados de Java 8 para Java 21, com atualização de dependências e
				frameworks.
				Tecnologias: Java, Spring Boot,
				CI/CD
				• Sustentação de microsserviços
				• refatoração do domínio de cartões
				""");

		StructuredResume content = ParsedResumeImporter.content(parsingResult(experience));

		assertThat(content.sections().getFirst().richLines()).containsExactly(
				List.of(new TextSpan("Porto | São Paulo–SP (remoto)", false)),
				List.of(new TextSpan(
						"Migração de 5 projetos legados de Java 8 para Java 21, com atualização de dependências e frameworks.",
						false)),
				List.of(new TextSpan("Tecnologias: Java, Spring Boot, CI/CD", false)),
				List.of(new TextSpan("Sustentação de microsserviços", false)),
				List.of(new TextSpan("refatoração do domínio de cartões", false)));
	}

	@Test
	void splitsExperienceIntoOneEntryPerJobWhenTheLayoutHasPeriods() {
		Section experience = new Section("Experiência Profissional", true, """
				Banco Porto - Desenvolvedor Sênior    mar 2021 - atual
				Porto | São Paulo–SP (remoto)
				• Migração de 5 projetos legados de Java 8 para Java 21
				• Sustentação de microsserviços
				Resultados:
				• Redução de 20% no custo de nuvem
				Tecnologias: Java, Spring Boot,
				CI/CD
				Acme Ltda
				01/2018 – 02/2021
				Desenvolvedor Pleno
				• Criou APIs REST
				""");

		List<ResumeEntry> entries = ParsedResumeImporter.content(parsingResult(experience)).sections().getFirst().entries();

		assertThat(entries).hasSize(2);
		assertThat(entries.get(0).heading()).isEqualTo("Banco Porto - Desenvolvedor Sênior");
		assertThat(entries.get(0).period()).isEqualTo("mar 2021 - atual");
		assertThat(entries.get(0).subheading()).isEqualTo("Porto | São Paulo–SP (remoto)");
		assertThat(entries.get(0).bullets()).hasSize(2);
		assertThat(entries.get(0).results()).hasSize(1);
		assertThat(entries.get(0).technologies()).isEqualTo("Java, Spring Boot, CI/CD");
		assertThat(entries.get(1).heading()).isEqualTo("Acme Ltda");
		assertThat(entries.get(1).period()).isEqualTo("01/2018 – 02/2021");
		assertThat(entries.get(1).subheading()).isEqualTo("Desenvolvedor Pleno");
		assertThat(entries.get(1).bullets()).hasSize(1);
	}

	@Test
	void readsTheCompanyRoleLocationHeadlineTheSystemWrites() {
		Section experience = new Section("Experiência Profissional", true, """
				BANCO PORTO — Desenvolvedor Sênior | São Paulo/SP
				03/2021 – Atual
				• Sustentação de microsserviços
				• Redução de 20% no custo de nuvem
				Tecnologias: Java
				""");

		ResumeEntry entry = ParsedResumeImporter.content(parsingResult(experience)).sections().getFirst().entries().getFirst();

		assertThat(entry.heading()).isEqualTo("Desenvolvedor Sênior");
		assertThat(entry.subheading()).isEqualTo("BANCO PORTO | São Paulo/SP");
		assertThat(entry.period()).isEqualTo("03/2021 – Atual");
		assertThat(entry.bullets()).hasSize(2);
		assertThat(entry.technologies()).isEqualTo("Java");
	}

	@Test
	void readsTheCourseInstitutionPeriodLineTheSystemWritesForEducation() {
		Section education = new Section("Formação Acadêmica", true, """
				Tecnólogo em Análise e Desenvolvimento de Sistemas — UNINTER | 2016 – 2018
				""");

		ResumeEntry entry = ParsedResumeImporter.content(parsingResult(education)).sections().getFirst().entries().getFirst();

		assertThat(entry.heading()).isEqualTo("Tecnólogo em Análise e Desenvolvimento de Sistemas");
		assertThat(entry.subheading()).isEqualTo("UNINTER");
		assertThat(entry.period()).isEqualTo("2016 – 2018");
	}

	@Test
	void readsLanguagesAsLanguageAndLevelPairs() {
		Section languages = new Section("IDIOMAS", true, "Inglês: Avançado\nEspanhol - Básico\nFrancês (Intermediário)\nLibras\n");

		ResumeSection section = ParsedResumeImporter.content(parsingResult(languages)).sections().getFirst();

		assertThat(section.kind()).isEqualTo(ResumeSectionKind.KEY_VALUE);
		assertThat(section.keyValues()).containsExactly(
				new com.diegohaefliger.atsresumeoptimizer.ai.KeyValueLine("Inglês", "Avançado"),
				new com.diegohaefliger.atsresumeoptimizer.ai.KeyValueLine("Espanhol", "Básico"),
				new com.diegohaefliger.atsresumeoptimizer.ai.KeyValueLine("Francês", "Intermediário"),
				new com.diegohaefliger.atsresumeoptimizer.ai.KeyValueLine("Libras", ""));
	}

	@Test
	void takesTheContactFromTheExtractedData() {
		ResumeContact contact = ParsedResumeImporter.contact(parsingResult());

		assertThat(contact.email()).isEqualTo("ana@email.com");
		assertThat(contact.phone()).isEqualTo("(55) 99999-0000");
	}

	@Test
	void fillsTheHeaderSlotsByPositionWhenTheValuesDoNotLookLikeContacts() {
		String raw = "Ana\nTitulo\nemail | 55 99999-0000 | LinkedIn | GitHub | Portfólio | \nLocalização\nObjetivo\nTexto\n";
		ParsingResult result = new ParsingResult(
				new NormalizedDocument(SourceFormat.PDF, raw, raw, 1),
				new ParsingSignals(false, false, false, false, false, false, false, false),
				List.of(new Section("Objetivo", true, "Texto")),
				new ContactInfo(Optional.empty(), Optional.of("(55) 99999-0000"), Optional.empty(), Optional.empty(),
						Optional.empty(), Optional.empty()));

		ResumeContact contact = ParsedResumeImporter.contact(result);

		assertThat(contact).isEqualTo(new ResumeContact("email", "(55) 99999-0000", "LinkedIn", "GitHub", "Portfólio",
				"Localização"));
	}

	private ParsingResult parsingResult() {
		return parsingResult(new Section("Resumo", true, "Backend com 6 anos de Java.\nFoco em APIs.\n"),
				new Section("Experiência Profissional", true, "• Criou APIs REST em Java\n- Reduziu custo de nuvem em 20%\n"));
	}

	private ParsingResult parsingResult(Section... sections) {
		return new ParsingResult(
				new NormalizedDocument(SourceFormat.PDF, RAW_TEXT, RAW_TEXT, 1),
				new ParsingSignals(false, false, false, false, false, false, false, false),
				List.of(sections),
				new ContactInfo(Optional.of("ana@email.com"), Optional.of("(55) 99999-0000"), Optional.empty(),
						Optional.empty(), Optional.empty(), Optional.empty()));
	}
}
