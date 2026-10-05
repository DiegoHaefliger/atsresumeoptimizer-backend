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

class PdfMultiColumnDetectorTest {

	private final PdfMultiColumnDetector detector = new PdfMultiColumnDetector();

	@Test
	void detectsSingleColumnResumeAsNotMultiColumn() throws IOException {
		InputStream pdf = pdfWithLinesAt(
			new Line("Ana Silva", 50, 720),
			new Line("Desenvolvedora Backend Java", 50, 700),
			new Line("Experiência Profissional", 50, 680),
			new Line("Empresa X - 2020 a 2023", 50, 660)
		);

		assertThat(detector.isMultiColumn(pdf)).isFalse();
	}

	@Test
	void detectsTwoStableLeftMarginsAsMultiColumn() throws IOException {
		InputStream pdf = pdfWithLinesAt(
			new Line("Skills", 50, 720),
			new Line("Java", 50, 700),
			new Line("SQL", 50, 680),
			new Line("Experiência Profissional", 300, 720),
			new Line("Empresa X - 2020 a 2023", 300, 700),
			new Line("Empresa Y - 2018 a 2020", 300, 680)
		);

		assertThat(detector.isMultiColumn(pdf)).isTrue();
	}

	@Test
	void ignoresOccasionalRightAlignedTextSuchAsADate() throws IOException {
		InputStream pdf = pdfWithLinesAt(
			new Line("Ana Silva", 50, 720),
			new Line("Desenvolvedora Backend Java", 50, 700),
			new Line("Empresa X", 50, 680),
			new Line("mar/2023", 400, 680)
		);

		assertThat(detector.isMultiColumn(pdf)).isFalse();
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
