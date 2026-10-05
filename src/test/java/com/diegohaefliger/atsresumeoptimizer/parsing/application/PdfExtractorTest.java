package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.NormalizedDocument;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class PdfExtractorTest {

	private final PdfExtractor extractor = new PdfExtractor();

	@Test
	void extractsSingleColumnResumeInReadingOrder() throws IOException {
		InputStream pdf = pdfWithLinesAt(new Line("Nome: Ana Silva", 50, 700), new Line("Email: ana@example.com", 50, 680));

		NormalizedDocument document = extractor.extract(pdf);

		assertThat(document.sourceFormat()).isEqualTo(SourceFormat.PDF);
		assertThat(document.rawText()).contains("Nome: Ana Silva").contains("Email: ana@example.com");
		assertThat(readingOrderIndex(document.rawText(), "Nome: Ana Silva")).isLessThan(readingOrderIndex(document.rawText(), "Email: ana@example.com"));
		assertThat(readingOrderIndex(document.structuredText(), "Nome: Ana Silva")).isLessThan(readingOrderIndex(document.structuredText(), "Email: ana@example.com"));
	}

	@Test
	void divergesBetweenRawAndStructuredOrderOnTwoColumnLayout() throws IOException {
		InputStream pdf = pdfWithLinesAt(
			new Line("Right-Line1", 300, 700),
			new Line("Left-Line1", 50, 700),
			new Line("Right-Line2", 300, 680),
			new Line("Left-Line2", 50, 680)
		);

		NormalizedDocument document = extractor.extract(pdf);

		assertThat(document.rawText()).isNotEqualTo(document.structuredText());
		assertThat(readingOrderIndex(document.rawText(), "Right-Line1")).isLessThan(readingOrderIndex(document.rawText(), "Left-Line1"));
		assertThat(readingOrderIndex(document.structuredText(), "Left-Line1")).isLessThan(readingOrderIndex(document.structuredText(), "Right-Line1"));
	}

	private int readingOrderIndex(String text, String needle) {
		int index = text.indexOf(needle);
		assertThat(index).as("'%s' presente em: %s", needle, text).isNotNegative();
		return index;
	}

	private record Line(String text, float x, float y) {
	}

	private InputStream pdfWithLinesAt(Line... lines) throws IOException {
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
			return new ByteArrayInputStream(out.toByteArray());
		}
	}
}
