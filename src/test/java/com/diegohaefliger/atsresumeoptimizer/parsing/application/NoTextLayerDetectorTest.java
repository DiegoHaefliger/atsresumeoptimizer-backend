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

class NoTextLayerDetectorTest {

	private final NoTextLayerDetector detector = new NoTextLayerDetector();

	@Test
	void resumeWithNormalTextIsNotFlagged() throws IOException {
		InputStream pdf = pdfWithText(
			"Ana Silva - Desenvolvedora Backend Java com mais de cinco anos de experiência em Spring Boot, "
				+ "PostgreSQL e arquitetura de microsserviços, atuando em times ágeis multidisciplinares."
		);

		assertThat(detector.hasNoTextLayer(pdf)).isFalse();
	}

	@Test
	void scannedPageWithoutTextLayerIsFlagged() throws IOException {
		InputStream pdf = pdfWithText("");

		assertThat(detector.hasNoTextLayer(pdf)).isTrue();
	}

	private InputStream pdfWithText(String text) throws IOException {
		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage(PDRectangle.LETTER);
			document.addPage(page);
			if (!text.isEmpty()) {
				try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
					stream.beginText();
					stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
					stream.newLineAtOffset(50, 700);
					stream.showText(text);
					stream.endText();
				}
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			document.save(out);
			return new ByteArrayInputStream(out.toByteArray());
		}
	}
}
