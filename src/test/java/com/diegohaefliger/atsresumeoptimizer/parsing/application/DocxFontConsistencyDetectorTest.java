package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

class DocxFontConsistencyDetectorTest {

	private final DocxFontConsistencyDetector detector = new DocxFontConsistencyDetector();

	@Test
	void singleFontAtNormalSizeIsNotFlagged() throws IOException {
		InputStream docx = docxWithRuns(document -> {
			addRun(document, "Ana Silva", "Calibri", 12);
			addRun(document, "Desenvolvedora Backend", "Calibri", 12);
		});

		assertThat(detector.hasInconsistentFonts(docx)).isFalse();
	}

	@Test
	void tooManyFontFamiliesIsFlagged() throws IOException {
		InputStream docx = docxWithRuns(document -> {
			addRun(document, "Ana Silva", "Calibri", 12);
			addRun(document, "Backend Java", "Arial", 12);
			addRun(document, "Spring Boot", "Times New Roman", 12);
			addRun(document, "PostgreSQL", "Comic Sans MS", 12);
		});

		assertThat(detector.hasInconsistentFonts(docx)).isTrue();
	}

	@Test
	void bodyFontBelowNinePointsIsFlagged() throws IOException {
		InputStream docx = docxWithRuns(document -> addRun(document, "Ana Silva", "Calibri", 7));

		assertThat(detector.hasInconsistentFonts(docx)).isTrue();
	}

	private void addRun(XWPFDocument document, String text, String fontFamily, double fontSize) {
		XWPFRun run = document.createParagraph().createRun();
		run.setText(text);
		run.setFontFamily(fontFamily);
		run.setFontSize(fontSize);
	}

	private InputStream docxWithRuns(java.util.function.Consumer<XWPFDocument> builder) throws IOException {
		try (XWPFDocument document = new XWPFDocument()) {
			builder.accept(document);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			document.write(out);
			return new ByteArrayInputStream(out.toByteArray());
		}
	}
}
