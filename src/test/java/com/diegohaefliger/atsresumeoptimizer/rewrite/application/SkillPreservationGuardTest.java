package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.ai.KeyValueLine;
import com.diegohaefliger.atsresumeoptimizer.ai.RemovedSkill;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import java.util.List;
import org.junit.jupiter.api.Test;

class SkillPreservationGuardTest {

	private static final List<Section> ORIGINAL = List.of(new Section("Competências Técnicas", true,
			"Competências: Java, Spring Boot, Docker, PostgreSQL, Python, Excel, Git, Linux, Formatação de computadores, "
					+ "Preparar café"));

	private final SkillPreservationGuard guard = new SkillPreservationGuard();

	@Test
	void restoresASkillThatDisappearedWithoutJustification() {
		StructuredResume output = withSkills("Java, Spring Boot, Docker, PostgreSQL, Python, Excel, Git, Linux, Preparar café",
				List.of());

		StructuredResume result = guard.apply(output, ORIGINAL, true);

		assertThat(result.sections().getFirst().keyValues()).containsExactly(new KeyValueLine("Competências",
				"Java, Spring Boot, Docker, PostgreSQL, Python, Excel, Git, Linux, Preparar café, Formatação de computadores"));
		assertThat(result.removedSkills()).isEmpty();
	}

	@Test
	void acceptsAJustifiedRemovalWithinTheCapWhenTheJobHighlightIsOn() {
		StructuredResume output = withSkills("Java, Spring Boot, Docker, PostgreSQL, Python, Excel, Git, Linux, Formatação de computadores",
				List.of(new RemovedSkill("preparar cafe", "Sem relação com desenvolvimento")));

		StructuredResume result = guard.apply(output, ORIGINAL, true);

		assertThat(result.removedSkills()).containsExactly(new RemovedSkill("Preparar café", "Sem relação com desenvolvimento"));
		assertThat(result.sections().getFirst().keyValues()).hasSize(1);
	}

	@Test
	void restoresEverythingWhenTheJobHighlightIsOff() {
		StructuredResume output = withSkills("Java, Spring Boot, Docker, PostgreSQL, Python, Excel, Git, Linux",
				List.of(new RemovedSkill("Preparar café", "Sem relação")));

		StructuredResume result = guard.apply(output, ORIGINAL, false);

		assertThat(result.removedSkills()).isEmpty();
		assertThat(result.sections().getFirst().keyValues().getFirst().value())
				.endsWith("Linux, Formatação de computadores, Preparar café");
	}

	@Test
	void undoesAllRemovalsWhenTheyExceedTheCap() {
		StructuredResume output = withSkills("Java, Spring Boot, Docker, PostgreSQL, Python, Linux", List.of(
				new RemovedSkill("Excel", "a"), new RemovedSkill("Git", "b"), new RemovedSkill("Preparar café", "c"),
				new RemovedSkill("Formatação de computadores", "d")));

		StructuredResume result = guard.apply(output, ORIGINAL, true);

		assertThat(result.removedSkills()).isEmpty();
		assertThat(result.sections().getFirst().keyValues().getLast().value())
				.isEqualTo("Java, Spring Boot, Docker, PostgreSQL, Python, Linux, Excel, Git, Formatação de computadores, "
						+ "Preparar café");
	}

	@Test
	void dropsReportedRemovalsOfSkillsTheOriginalNeverHad() {
		StructuredResume output = withSkills("Java, Spring Boot, Docker, PostgreSQL, Python, Excel, Git, Linux, "
				+ "Formatação de computadores, Preparar café", List.of(new RemovedSkill("Photoshop", "Sem relação")));

		assertThat(guard.apply(output, ORIGINAL, true).removedSkills()).isEmpty();
	}

	@Test
	void createsASkillsSectionWhenTheAiDroppedItEntirely() {
		StructuredResume output = new StructuredResume("Ana", null, List.of());

		StructuredResume result = guard.apply(output, ORIGINAL, false);

		assertThat(result.sections()).singleElement()
				.satisfies(section -> assertThat(section.semanticType()).isEqualTo(ResumeSectionSemanticType.SKILLS));
	}

	@Test
	void putsEachRestoredSkillBackInItsOriginalGroupAndJoinsWrappedLines() {
		List<Section> original = List.of(new Section("COMPETÊNCIAS TÉCNICAS", true, """
				Linguagens: Java (8 a 25), SQL, Python, Delphi
				Arquitetura: Microsserviços, APIs REST
				Domínios: Cartões, meios de pagamento, migração de
				projetos legados"""));
		ResumeSection section = new ResumeSection("COMPETÊNCIAS", ResumeSectionSemanticType.SKILLS, ResumeSectionKind.KEY_VALUE,
				null, List.of(new KeyValueLine("Linguagens", "Java (8 a 25), SQL"),
						new KeyValueLine("Arquitetura", "Microsserviços, APIs REST")), null, null);

		StructuredResume result = guard.apply(new StructuredResume("Ana", null, List.of(section)), original, false);

		assertThat(result.sections().getFirst().keyValues()).containsExactly(
				new KeyValueLine("Linguagens", "Java (8 a 25), SQL, Python, Delphi"),
				new KeyValueLine("Arquitetura", "Microsserviços, APIs REST"),
				new KeyValueLine("Domínios", "Cartões, meios de pagamento, migração de projetos legados"));
	}

	@Test
	void doesNotRestoreAnItemJoinedByAndWhenTheAiListedItsPartsSeparately() {
		List<Section> original = List.of(new Section("COMPETÊNCIAS TÉCNICAS", true,
				"Banco de dados: PostgreSQL, Oracle, SQL Server, MySQL e MongoDB"));
		ResumeSection section = new ResumeSection("COMPETÊNCIAS", ResumeSectionSemanticType.SKILLS, ResumeSectionKind.KEY_VALUE,
				null, List.of(new KeyValueLine("Banco de dados", "PostgreSQL, Oracle, SQL Server, MySQL, MongoDB")), null, null);

		StructuredResume result = guard.apply(new StructuredResume("Ana", null, List.of(section)), original, true);

		assertThat(result.sections().getFirst().keyValues())
				.containsExactly(new KeyValueLine("Banco de dados", "PostgreSQL, Oracle, SQL Server, MySQL, MongoDB"));
	}

	private StructuredResume withSkills(String skills, List<RemovedSkill> removed) {
		ResumeSection section = new ResumeSection("COMPETÊNCIAS", ResumeSectionSemanticType.SKILLS, ResumeSectionKind.KEY_VALUE,
				null, List.of(new KeyValueLine("Competências", skills)), null, null);
		return new StructuredResume("Ana", null, List.of(section), removed);
	}
}
