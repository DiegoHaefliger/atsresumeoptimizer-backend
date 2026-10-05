package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class ResumeAnalysisPipelineTest {

	private final ResumeAnalysisPipeline pipeline = new ResumeAnalysisPipeline(
			new DocumentMimeValidator(), new SectionSegmenter(), new ContactExtractor(),
			new IconGlyphDetector(), new NoTextLayerDetector());

	@Test
	void runsExtractionDetectorsAndSegmentationOverASingleColumnResume() throws IOException {
		byte[] pdf = pdfWithLinesAt(
				new Line("Ana Silva", 50, 750),
				new Line("ana.silva@email.com", 50, 730),
				new Line("EXPERIENCIA PROFISSIONAL", 50, 700),
				new Line("Desenvolvedora Backend Java na Empresa X, 2020 a 2023", 50, 680));

		ParsingResult result = pipeline.analyze(pdf);

		assertThat(result.document().rawText()).contains("Ana Silva");
		assertThat(result.signals().noTextLayer()).isFalse();
		assertThat(result.signals().multiColumn()).isFalse();
		assertThat(result.contact().email()).contains("ana.silva@email.com");
		assertThat(result.sections())
				.anyMatch(section -> section.recognized() && section.title().equalsIgnoreCase("EXPERIENCIA PROFISSIONAL"));
	}

	@Test
	void runsExtractionDetectorsAndSegmentationOverAnOdtResume() throws Exception {
		byte[] odt = OdtTestFixture.odt()
				.withParagraphs("Ana Silva", "ana.silva@email.com", "EXPERIENCIA PROFISSIONAL",
						"Desenvolvedora Backend Java na Empresa X, 2020 a 2023")
				.build();

		ParsingResult result = pipeline.analyze(odt);

		assertThat(result.document().rawText()).contains("Ana Silva");
		assertThat(result.signals().multiColumn()).isFalse();
		assertThat(result.contact().email()).contains("ana.silva@email.com");
		assertThat(result.sections())
				.anyMatch(section -> section.recognized() && section.title().equalsIgnoreCase("EXPERIENCIA PROFISSIONAL"));
	}

	private record Line(String text, float x, float y) {
	}

	private byte[] pdfWithLinesAt(Line... lines) throws IOException {
		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage(PDRectangle.LETTER);
			document.addPage(page);
			try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
				for (Line line : lines) {
					stream.beginText();
					stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
					stream.newLineAtOffset(line.x(), line.y());
					stream.showText(line.text());
					stream.endText();
				}
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			document.save(out);
			return out.toByteArray();
		}
	}
}
