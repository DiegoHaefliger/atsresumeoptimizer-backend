package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import com.diegohaefliger.atsresumeoptimizer.parsing.application.BulletTextExtractor;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RewriteGuardRailsTest {

	private final RewriteGuardRails guardRails = new RewriteGuardRails(job -> text -> Set.of(), new BulletTextExtractor());

	@Test
	void failsWhenTheAiDroppedAWholeJob() {
		ResumeEntry analyst = new ResumeEntry("Analista de Sistemas", "2012 – 2022", "Fockink | Panambi–RS", null,
				List.of(List.of(new TextSpan("Reescrita do sistema legado de Delphi para Java.", false)),
						List.of(new TextSpan("Suporte técnico ao time comercial em apresentações a clientes.", false))),
				null);

		assertThatThrownBy(() -> guardRails.enforce(experience(analyst), TWO_JOBS_TEXT, TWO_JOBS_SECTIONS, null, true, List.of()))
				.isInstanceOf(DroppedContentException.class)
				.hasMessageContaining("1 de 3");
	}

	@Test
	void failsWhenTheAiDroppedTheProjectsSection() {
		List<Section> original = List.of(new Section("Projetos", true, "Argos\nPlataforma de vagas com IA."));
		StructuredResume output = new StructuredResume("Ana", null, List.of());

		assertThatThrownBy(() -> guardRails.enforce(output, "Projetos\nArgos", original, null, true, List.of()))
				.isInstanceOf(DroppedContentException.class)
				.hasMessageContaining("projetos");
	}

	private static final String TWO_JOBS_TEXT = """
			EXPERIÊNCIA PROFISSIONAL
			Analista de Sistemas — 2012 – 2022
			Fockink | Panambi–RS
			• Reescrita do sistema legado de Delphi para Java.
			• Suporte técnico ao time comercial em apresentações a clientes.
			Programador — 2011 – 2012
			Lógica Informática | Panambi–RS
			• Desenvolvimento de módulos de ERP em GeneXus.
			""";

	private static final List<Section> TWO_JOBS_SECTIONS = List.of(new Section("Experiência Profissional", true, """
			Analista de Sistemas — 2012 – 2022
			Fockink | Panambi–RS
			• Reescrita do sistema legado de Delphi para Java.
			• Suporte técnico ao time comercial em apresentações a clientes.
			Programador — 2011 – 2012
			Lógica Informática | Panambi–RS
			• Desenvolvimento de módulos de ERP em GeneXus."""));

	@Test
	void restoresTheDroppedBulletIntoTheEntryItCameFrom() {
		ResumeEntry analyst = new ResumeEntry("Analista de Sistemas", "2012 – 2022", "Fockink | Panambi–RS", null,
				List.of(List.of(new TextSpan("Reescrita do sistema legado de Delphi para Java.", false))), null);
		ResumeEntry programmer = new ResumeEntry("Programador", "2011 – 2012", "Lógica Informática | Panambi–RS", null,
				List.of(List.of(new TextSpan("Desenvolvimento de módulos de ERP em GeneXus.", false))), null);

		StructuredResume guarded = guardRails.enforce(experience(analyst, programmer), TWO_JOBS_TEXT, TWO_JOBS_SECTIONS,
				null, true, List.of());

		List<ResumeEntry> entries = guarded.sections().getFirst().entries();
		assertThat(entries.get(0).bullets()).extracting(bullet -> bullet.getFirst().text())
				.containsExactly("Reescrita do sistema legado de Delphi para Java.",
						"Suporte técnico ao time comercial em apresentações a clientes.");
		assertThat(entries.get(1).bullets()).hasSize(1);
	}

	@Test
	void failsWhenTheAiMergedTwoBulletsIntoOne() {
		ResumeEntry analyst = new ResumeEntry("Analista de Sistemas", "2012 – 2022", "Fockink | Panambi–RS", null,
				List.of(List.of(new TextSpan("Reescrita do sistema legado de Delphi para Java.", false),
						new TextSpan("Suporte técnico ao time comercial em apresentações a clientes.", false))),
				null);
		ResumeEntry programmer = new ResumeEntry("Programador", "2011 – 2012", "Lógica Informática | Panambi–RS", null,
				List.of(List.of(new TextSpan("Desenvolvimento de módulos de ERP em GeneXus.", false))), null);

		assertThatThrownBy(() -> guardRails.enforce(experience(analyst, programmer), TWO_JOBS_TEXT, TWO_JOBS_SECTIONS,
				null, true, List.of()))
				.isInstanceOf(DroppedContentException.class)
				.hasMessageContaining("1 de 3");
	}

	@Test
	void removesBoldFromExperienceBullets() {
		ResumeEntry programmer = new ResumeEntry("Programador", "2011 – 2012", "Lógica Informática | Panambi–RS", null,
				List.of(List.of(new TextSpan("Desenvolvimento de módulos de ", false), new TextSpan("ERP em GeneXus.", true))),
				null);
		List<Section> original = List.of(new Section("Experiência Profissional", true,
				"Programador\n• Desenvolvimento de módulos de ERP em GeneXus."));

		StructuredResume guarded = guardRails.enforce(experience(programmer), "texto", original, null, true, List.of());

		assertThat(guarded.sections().getFirst().entries().getFirst().bullets().getFirst())
				.extracting(TextSpan::bold)
				.containsOnly(false);
	}

	private static StructuredResume experience(ResumeEntry... entries) {
		return new StructuredResume("Ana", null, List.of(new ResumeSection("EXPERIÊNCIA",
				ResumeSectionSemanticType.EXPERIENCE, ResumeSectionKind.ENTRIES, null, null, null, List.of(entries))));
	}
}
