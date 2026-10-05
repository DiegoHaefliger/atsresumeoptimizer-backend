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

class PdfTableDetectorTest {

	private final PdfTableDetector detector = new PdfTableDetector();

	@Test
	void plainTextResumeIsNotFlagged() throws IOException {
		InputStream pdf = pdfWithPlainText();

		assertThat(detector.hasTable(pdf)).isFalse();
	}

	@Test
	void gridOfDrawnCellsIsFlaggedAsTable() throws IOException {
		InputStream pdf = pdfWithCellGrid();

		assertThat(detector.hasTable(pdf)).isTrue();
	}

	private InputStream pdfWithPlainText() throws IOException {
		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage(PDRectangle.LETTER);
			document.addPage(page);
			try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
				stream.beginText();
				stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
				stream.newLineAtOffset(50, 700);
				stream.showText("Ana Silva - Desenvolvedora Backend Java");
				stream.endText();
			}
			return toInputStream(document);
		}
	}

	private InputStream pdfWithCellGrid() throws IOException {
		try (PDDocument document = new PDDocument()) {
			PDPage page = new PDPage(PDRectangle.LETTER);
			document.addPage(page);
			try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
				float[] rowsY = { 700, 680, 660 };
				for (float y : rowsY) {
					stream.addRect(50, y, 100, 15);
					stream.addRect(150, y, 100, 15);
					stream.stroke();
				}
			}
			return toInputStream(document);
		}
	}

	private InputStream toInputStream(PDDocument document) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		document.save(out);
		return new ByteArrayInputStream(out.toByteArray());
	}
}
