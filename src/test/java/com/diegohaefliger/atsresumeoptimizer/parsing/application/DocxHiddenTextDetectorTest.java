package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.junit.jupiter.api.Test;

class DocxHiddenTextDetectorTest {

	private final DocxHiddenTextDetector detector = new DocxHiddenTextDetector();

	@Test
	void normalTextIsNotFlagged() throws IOException {
		InputStream docx = docxWithRun(run -> run.setText("Java Spring Boot PostgreSQL"));

		assertThat(detector.hasHiddenText(docx)).isFalse();
	}

	@Test
	void vanishRunIsFlagged() throws IOException {
		InputStream docx = docxWithRun(run -> {
			run.setText("Kubernetes Kafka Terraform");
			run.setVanish(true);
		});

		assertThat(detector.hasHiddenText(docx)).isTrue();
	}

	@Test
	void whiteFontColorIsFlagged() throws IOException {
		InputStream docx = docxWithRun(run -> {
			run.setText("Kubernetes Kafka Terraform");
			run.setColor("FFFFFF");
		});

		assertThat(detector.hasHiddenText(docx)).isTrue();
	}

	private InputStream docxWithRun(java.util.function.Consumer<XWPFRun> runCustomizer) throws IOException {
		try (XWPFDocument document = new XWPFDocument()) {
			XWPFRun run = document.createParagraph().createRun();
			runCustomizer.accept(run);
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			document.write(out);
			return new ByteArrayInputStream(out.toByteArray());
		}
	}
}
