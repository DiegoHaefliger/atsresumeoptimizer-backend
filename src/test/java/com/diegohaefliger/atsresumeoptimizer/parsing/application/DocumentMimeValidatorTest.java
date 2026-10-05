package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.UnsupportedDocumentTypeException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

class DocumentMimeValidatorTest {

	private final DocumentMimeValidator validator = new DocumentMimeValidator();

	@Test
	void detectsRealPdfContentEvenWithWrongExtension() throws IOException {
		assertThat(validator.detectSupportedFormat(pdfBytes())).isEqualTo(SourceFormat.PDF);
	}

	@Test
	void detectsRealDocxContent() throws IOException {
		assertThat(validator.detectSupportedFormat(docxBytes())).isEqualTo(SourceFormat.DOCX);
	}

	@Test
	void detectsRealOdtContent() throws IOException {
		assertThat(validator.detectSupportedFormat(OdtTestFixture.odt().build())).isEqualTo(SourceFormat.ODT);
	}

	@Test
	void rejectsContentThatIsNotActuallyPdfOrDocxRegardlessOfDeclaredType() {
		byte[] renamedTextFile = "não sou um PDF, só tenho a extensão .pdf".getBytes(StandardCharsets.UTF_8);

		assertThatThrownBy(() -> validator.detectSupportedFormat(renamedTextFile))
			.isInstanceOf(UnsupportedDocumentTypeException.class);
	}

	private byte[] pdfBytes() throws IOException {
		try (PDDocument document = new PDDocument()) {
			document.addPage(new PDPage());
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			document.save(out);
			return out.toByteArray();
		}
	}

	private byte[] docxBytes() throws IOException {
		try (XWPFDocument document = new XWPFDocument()) {
			document.createParagraph().createRun().setText("Ana Silva");
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			document.write(out);
			return out.toByteArray();
		}
	}
}
