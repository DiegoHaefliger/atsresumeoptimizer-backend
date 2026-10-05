package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.ai.ResumeEntry;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSection;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionKind;
import com.diegohaefliger.atsresumeoptimizer.ai.ResumeSectionSemanticType;
import com.diegohaefliger.atsresumeoptimizer.ai.StructuredResume;
import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ThemedPdfRendererTest {

	private static final List<ContactField> CONTACT =
			ContactField.collect("ana@email.com | (55) 99999-0000", "github.com/anasilva", "https://ana.dev", "Panambi, RS");

	private final ThemedPdfRenderer renderer = new ThemedPdfRenderer();

	@ParameterizedTest
	@EnumSource(ResumeTemplate.class)
	void writesTheSameContentForEveryTemplate(ResumeTemplate template) throws Exception {
		ResumeEntry entry = new ResumeEntry("Desenvolvedora Backend", "2020 - 2023", "Empresa X", null,
				List.of(List.of(new TextSpan("Reduzi bugs em ", false), new TextSpan("40%", true))), "Java, Spring Boot");
		StructuredResume content = new StructuredResume("Ana Silva", "Backend Java", List.of(experience(entry)));

		byte[] bytes = renderer.render(ResumeTheme.of(template), content, CONTACT);

		try (PDDocument document = Loader.loadPDF(bytes)) {
			assertThat(document.getNumberOfPages()).isEqualTo(1);
			assertThat(new PDFTextStripper().getText(document))
					.contains("Ana Silva", "Backend Java", "ana@email.com", "github.com/anasilva", "Panambi, RS",
							"EXPERIENCIA", "Desenvolvedora Backend", "2020 – 2023", "EMPRESA X", "Reduzi bugs em 40%");
		}
	}

	@ParameterizedTest
	@EnumSource(ResumeTemplate.class)
	void rendersStyledTextAndListsWithoutMarkupLeftovers(ResumeTemplate template) throws Exception {
		ResumeSection summary = new ResumeSection("RESUMO", ResumeSectionSemanticType.SUMMARY, ResumeSectionKind.PARAGRAPH,
				"Texto **forte** e *inclinado* e ++sublinhado++\n- primeiro item\n2. segundo item", null, null, null);

		byte[] bytes = renderer.render(ResumeTheme.of(template), new StructuredResume("Ana Silva", null, List.of(summary)),
				CONTACT);

		try (PDDocument document = Loader.loadPDF(bytes)) {
			String text = new PDFTextStripper().getText(document);
			assertThat(text).contains("Texto forte e inclinado e sublinhado", "• primeiro item", "2. segundo item");
			assertThat(text).doesNotContain("**", "++");
		}
	}

	@ParameterizedTest
	@EnumSource(ResumeTemplate.class)
	void rendersAContactFieldThatSpansTwoLines(ResumeTemplate template) throws Exception {
		List<ContactField> contact = ContactField.collect("ana@email.com", "", "", "São Paulo–SP\n(remoto)");
		StructuredResume content = new StructuredResume("Ana Silva", null, List.of());

		byte[] bytes = renderer.render(ResumeTheme.of(template), content, contact);

		try (PDDocument document = Loader.loadPDF(bytes)) {
			assertThat(new PDFTextStripper().getText(document)).contains("(remoto)");
		}
	}

	@ParameterizedTest
	@EnumSource(ResumeTemplate.class)
	void wrapsALongEntryHeadingInsideThePage(ResumeTemplate template) throws Exception {
		ResumeEntry entry = new ResumeEntry(
				"Desenvolvedor Backend Sênior responsável pela plataforma de pagamentos da Empresa Muito Grande de Tecnologia",
				"Jan 2020 - Atual", "Empresa X", null, List.of(), null);
		StructuredResume content = new StructuredResume("Ana Silva", null, List.of(experience(entry)));

		byte[] bytes = renderer.render(ResumeTheme.of(template), content, CONTACT);

		try (PDDocument document = Loader.loadPDF(bytes)) {
			float pageWidth = document.getPage(0).getMediaBox().getWidth();
			assertThat(rightmostGlyphEdge(document)).isLessThan(pageWidth);
			assertThat(new PDFTextStripper().getText(document)).contains("Jan 2020 – Atual");
		}
	}

	@ParameterizedTest
	@EnumSource(ResumeTemplate.class)
	void paginatesWhenContentOverflowsASinglePage(ResumeTemplate template) throws Exception {
		List<List<TextSpan>> manyBullets = IntStream.range(0, 80)
				.<List<TextSpan>>mapToObj(i -> List.of(new TextSpan(
						"Linha de experiencia profissional numero " + i + " com bastante texto pra forcar quebra de linha", false)))
				.toList();
		ResumeEntry entry = new ResumeEntry("Cargo", null, null, null, manyBullets, null);
		StructuredResume content = new StructuredResume("Ana Silva", null, List.of(experience(entry)));

		byte[] bytes = renderer.render(ResumeTheme.of(template), content, CONTACT);

		try (PDDocument document = Loader.loadPDF(bytes)) {
			assertThat(document.getNumberOfPages()).isGreaterThan(1);
			assertThat(new PDFTextStripper().getText(document))
					.contains("Linha de experiencia profissional numero 0", "Linha de experiencia profissional numero 79");
		}
	}

	private ResumeSection experience(ResumeEntry entry) {
		return new ResumeSection(
				"EXPERIENCIA", ResumeSectionSemanticType.EXPERIENCE, ResumeSectionKind.ENTRIES, null, null, null, List.of(entry));
	}

	private float rightmostGlyphEdge(PDDocument document) throws IOException {
		List<Float> edges = new ArrayList<>();
		PDFTextStripper stripper = new PDFTextStripper() {
			@Override
			protected void writeString(String text, List<TextPosition> textPositions) {
				textPositions.forEach(position -> edges.add(position.getXDirAdj() + position.getWidthDirAdj()));
			}
		};
		stripper.getText(document);
		return edges.stream().max(Float::compare).orElse(0f);
	}
}
