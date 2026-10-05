package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ThemedDocxRendererTest {

	private static final List<ContactField> CONTACT =
			ContactField.collect("ana@email.com", "github.com/anasilva", "https://ana.dev", "Panambi, RS, Brasil");

	private final ThemedDocxRenderer renderer = new ThemedDocxRenderer();

	@ParameterizedTest
	@EnumSource(ResumeTemplate.class)
	void writesNameContactSectionAndEntryInTheSameParagraphOrder(ResumeTemplate template) throws Exception {
		List<XWPFParagraph> paragraphs = paragraphs(renderer.render(ResumeTheme.of(template), sample(), CONTACT));

		assertThat(paragraphs.get(0).getText()).isEqualTo("Ana Silva");
		assertThat(paragraphs.get(1).getText()).contains("ana@email.com", "github.com/anasilva", "https://ana.dev",
				"Panambi, RS, Brasil");
		assertThat(paragraphs.get(1).getRuns().stream().filter(run -> run instanceof XWPFHyperlinkRun).count()).isEqualTo(3);
		assertThat(paragraphs.get(2).getText()).isEqualTo("EXPERIENCIA");
		assertThat(paragraphs.get(3).getText()).isEqualTo("EMPRESA X — Desenvolvedora Backend");
		assertThat(paragraphs.get(4).getText()).isEqualTo("2020 – 2023");
		assertThat(paragraphs.get(5).getText()).isEqualTo("•  Reduzi bugs em 40%");
	}

	@Test
	void templatesDifferOnlyInSkinNotInStructure() throws Exception {
		List<XWPFParagraph> classic = paragraphs(renderer.render(ResumeTheme.of(ResumeTemplate.CLASSIC), sample(), CONTACT));
		List<XWPFParagraph> modern =
				paragraphs(renderer.render(ResumeTheme.of(ResumeTemplate.MODERN_BLUE), sample(), CONTACT));

		assertThat(classic).hasSameSizeAs(modern);
		for (int i = 2; i < classic.size(); i++) {
			assertThat(classic.get(i).getText()).isEqualTo(modern.get(i).getText());
		}
	}

	@Test
	void keepsTheBlankLineTheCandidateLeftBetweenItems() throws Exception {
		ResumeSection projects = new ResumeSection("PROJETOS", ResumeSectionSemanticType.PROJECTS,
				ResumeSectionKind.RICH_LINES, null, null,
				List.of(List.of(new TextSpan("Projeto A", false)), List.of(new TextSpan("", false)),
						List.of(new TextSpan("Projeto B", false))),
				null);
		ResumeSection summary = new ResumeSection("RESUMO", ResumeSectionSemanticType.SUMMARY,
				ResumeSectionKind.PARAGRAPH, "Linha 1\n\nLinha 2", null, null, null);

		List<String> texts = paragraphs(renderer.render(ResumeTheme.of(ResumeTemplate.CLASSIC),
				new StructuredResume("Ana Silva", null, List.of(summary, projects)), CONTACT))
				.stream().map(XWPFParagraph::getText).toList();

		assertThat(texts).containsSequence("Linha 1", "", "Linha 2");
		assertThat(texts).containsSequence("Projeto A", "", "Projeto B");
	}

	private StructuredResume sample() {
		ResumeEntry entry = new ResumeEntry("Desenvolvedora Backend", "2020 - 2023", "Empresa X", null,
				List.of(List.of(new TextSpan("Reduzi bugs em ", false), new TextSpan("40%", true))), null);
		ResumeSection section = new ResumeSection(
				"EXPERIENCIA", ResumeSectionSemanticType.EXPERIENCE, ResumeSectionKind.ENTRIES, null, null, null, List.of(entry));
		return new StructuredResume("Ana Silva", null, List.of(section));
	}

	private List<XWPFParagraph> paragraphs(byte[] bytes) throws IOException {
		try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
			return List.copyOf(document.getParagraphs());
		}
	}
}
