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
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.junit.jupiter.api.Test;

class PdfHiddenTextDetectorTest {

	private final PdfHiddenTextDetector detector = new PdfHiddenTextDetector();

	@Test
	void normalBlackTextIsNotFlagged() throws IOException {
		InputStream pdf = pdfWithText(stream -> {
			stream.showText("Java Spring Boot PostgreSQL");
		});

		assertThat(detector.hasHiddenText(pdf)).isFalse();
	}

	@Test
	void whiteOnWhiteTextIsFlagged() throws IOException {
		InputStream pdf = pdfWithText(stream -> {
			stream.setNonStrokingColor(1f, 1f, 1f);
			stream.showText("Kubernetes Kafka Terraform Kubernetes Kafka Terraform");
		});

		assertThat(detector.hasHiddenText(pdf)).isTrue();
	}

	@Test
	void invisibleRenderingModeIsFlagged() throws IOException {
		InputStream pdf = pdfWithText(stream -> {
			stream.setRenderingMode(RenderingMode.NEITHER);
			stream.showText("Kubernetes Kafka Terraform Kubernetes Kafka Terraform");
		});

		assertThat(detector.hasHiddenText(pdf)).isTrue();
	}

	private interface TextPainter {
		void paint(PDPageContentStream stream) throws IOException;
	}

	private InputStream pdfWithText(TextPainter painter) throws IOException {
		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage(PDRectangle.LETTER);
			document.addPage(page);
			try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
				stream.beginText();
				stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
				stream.newLineAtOffset(50, 700);
				painter.paint(stream);
				stream.endText();
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			document.save(out);
			return new ByteArrayInputStream(out.toByteArray());
		}
	}
}
