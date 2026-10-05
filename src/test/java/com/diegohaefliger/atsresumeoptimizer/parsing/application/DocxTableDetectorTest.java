package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

class DocxTableDetectorTest {

	private final DocxTableDetector detector = new DocxTableDetector();

	@Test
	void plainParagraphsAreNotFlagged() throws IOException {
		InputStream docx = docxWith(document -> document.createParagraph().createRun().setText("Ana Silva"));

		assertThat(detector.hasTable(docx)).isFalse();
	}

	@Test
	void tableWithContentIsFlagged() throws IOException {
		InputStream docx = docxWith(document -> {
			var table = document.createTable(2, 2);
			table.getRow(0).getCell(0).setText("Cargo");
			table.getRow(0).getCell(1).setText("Empresa");
			table.getRow(1).getCell(0).setText("Dev Backend");
			table.getRow(1).getCell(1).setText("Empresa X");
		});

		assertThat(detector.hasTable(docx)).isTrue();
	}

	private InputStream docxWith(java.util.function.Consumer<XWPFDocument> builder) throws IOException {
		try (XWPFDocument document = new XWPFDocument()) {
			builder.accept(document);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			document.write(out);
			return new ByteArrayInputStream(out.toByteArray());
		}
	}
}
