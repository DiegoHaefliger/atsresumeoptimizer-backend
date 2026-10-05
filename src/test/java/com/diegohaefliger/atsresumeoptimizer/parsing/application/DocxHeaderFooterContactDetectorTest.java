package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.wp.usermodel.HeaderFooterType;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

class DocxHeaderFooterContactDetectorTest {

	private final DocxHeaderFooterContactDetector detector = new DocxHeaderFooterContactDetector();

	@Test
	void resumeWithoutHeaderIsNotFlagged() throws IOException {
		InputStream docx = docxWith(document -> document.createParagraph().createRun().setText("Ana Silva"));

		assertThat(detector.hasContactInHeaderOrFooter(docx)).isFalse();
	}

	@Test
	void emailInHeaderIsFlagged() throws IOException {
		InputStream docx = docxWith(document -> {
			var header = document.createHeader(HeaderFooterType.DEFAULT);
			header.createParagraph().createRun().setText("ana.silva@example.com");
		});

		assertThat(detector.hasContactInHeaderOrFooter(docx)).isTrue();
	}

	@Test
	void phoneInFooterIsFlagged() throws IOException {
		InputStream docx = docxWith(document -> {
			var footer = document.createFooter(HeaderFooterType.DEFAULT);
			footer.createParagraph().createRun().setText("(11) 99999-0000");
		});

		assertThat(detector.hasContactInHeaderOrFooter(docx)).isTrue();
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
