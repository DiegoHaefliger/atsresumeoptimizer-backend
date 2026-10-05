package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class PdfFontConsistencyDetectorTest {

	private final PdfFontConsistencyDetector detector = new PdfFontConsistencyDetector();

	@Test
	void singleFontAtNormalSizeIsNotFlagged() throws IOException {
		InputStream pdf = pdfWithLines(new Line("Ana Silva", Standard14Fonts.FontName.HELVETICA, 12), new Line("Desenvolvedora Backend", Standard14Fonts.FontName.HELVETICA, 12));

		assertThat(detector.hasInconsistentFonts(pdf)).isFalse();
	}

	@Test
	void tooManyFontFamiliesIsFlagged() throws IOException {
		InputStream pdf = pdfWithLines(
			new Line("Ana Silva", Standard14Fonts.FontName.HELVETICA, 12),
			new Line("Backend Java", Standard14Fonts.FontName.TIMES_ROMAN, 12),
			new Line("Spring Boot", Standard14Fonts.FontName.COURIER, 12),
			new Line("PostgreSQL", Standard14Fonts.FontName.HELVETICA_BOLD, 12)
		);

		assertThat(detector.hasInconsistentFonts(pdf)).isTrue();
	}

	@Test
	void bodyFontBelowNinePointsIsFlagged() throws IOException {
		InputStream pdf = pdfWithLines(new Line("Ana Silva", Standard14Fonts.FontName.HELVETICA, 7));

		assertThat(detector.hasInconsistentFonts(pdf)).isTrue();
	}

	private record Line(String text, Standard14Fonts.FontName font, float size) {
	}

	private InputStream pdfWithLines(Line... lines) throws IOException {
		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage(PDRectangle.LETTER);
			document.addPage(page);
			try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
				float y = 700;
				for (Line line : lines) {
					PDFont font = new PDType1Font(line.font());
					stream.beginText();
					stream.setFont(font, line.size());
					stream.newLineAtOffset(50, y);
					stream.showText(line.text());
					stream.endText();
					y -= 20;
				}
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			document.save(out);
			return new ByteArrayInputStream(out.toByteArray());
		}
	}
}
