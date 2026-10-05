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
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

class PdfHeaderFooterContactDetectorTest {

	private final PdfHeaderFooterContactDetector detector = new PdfHeaderFooterContactDetector();

	@Test
	void singlePageResumeIsNeverFlagged() throws IOException {
		InputStream pdf = pdfWithPages(1, "ana.silva@example.com");

		assertThat(detector.hasContactInHeaderOrFooter(pdf)).isFalse();
	}

	@Test
	void emailRepeatedOnTopOfEveryPageIsFlagged() throws IOException {
		InputStream pdf = pdfWithPages(3, "ana.silva@example.com");

		assertThat(detector.hasContactInHeaderOrFooter(pdf)).isTrue();
	}

	@Test
	void bodyTextWithoutContactInfoIsNotFlagged() throws IOException {
		InputStream pdf = pdfWithPages(3, "Experiência Profissional");

		assertThat(detector.hasContactInHeaderOrFooter(pdf)).isFalse();
	}

	private InputStream pdfWithPages(int pageCount, String topBandText) throws IOException {
		try (PDDocument document = new PDDocument()) {
			for (int i = 0; i < pageCount; i++) {
				PDPage page = new PDPage(PDRectangle.LETTER);
				document.addPage(page);
				try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
					stream.beginText();
					stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
					stream.newLineAtOffset(50, page.getMediaBox().getHeight() - 30);
					stream.showText(topBandText);
					stream.endText();

					stream.beginText();
					stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
					stream.newLineAtOffset(50, 400);
					stream.showText("Conteúdo da página " + (i + 1));
					stream.endText();
				}
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			document.save(out);
			return new ByteArrayInputStream(out.toByteArray());
		}
	}
}
