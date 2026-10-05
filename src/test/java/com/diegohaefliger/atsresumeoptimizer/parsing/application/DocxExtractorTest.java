package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.NormalizedDocument;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

class DocxExtractorTest {

	private final DocxExtractor extractor = new DocxExtractor();

	@Test
	void extractsParagraphsInReadingOrder() throws IOException {
		InputStream docx = docxWith(document -> {
			addParagraph(document, "Ana Silva");
			addParagraph(document, "Desenvolvedora Backend Java");
		});

		NormalizedDocument document = extractor.extract(docx);

		assertThat(document.sourceFormat()).isEqualTo(SourceFormat.DOCX);
		assertThat(document.rawText()).contains("Ana Silva").contains("Desenvolvedora Backend Java");
		assertThat(document.structuredText()).contains("Ana Silva").contains("Desenvolvedora Backend Java");
	}

	@Test
	void extractsContactTableInHeaderIntoStructuredRows() throws IOException {
		InputStream docx = docxWith(document -> {
			XWPFTable table = document.createTable(1, 3);
			table.getRow(0).getCell(0).setText("Ana Silva");
			table.getRow(0).getCell(1).setText("ana@example.com");
			table.getRow(0).getCell(2).setText("(11) 99999-0000");
			addParagraph(document, "Resumo profissional na área de backend.");
		});

		NormalizedDocument document = extractor.extract(docx);

		assertThat(document.structuredText()).contains("Ana Silva | ana@example.com | (11) 99999-0000");
		assertThat(document.rawText()).contains("Ana Silva").contains("ana@example.com").contains("(11) 99999-0000");
	}

	private void addParagraph(XWPFDocument document, String text) {
		XWPFParagraph paragraph = document.createParagraph();
		XWPFRun run = paragraph.createRun();
		run.setText(text);
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
