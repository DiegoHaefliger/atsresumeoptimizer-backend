package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

class DocxMultiColumnDetectorTest {

	private final DocxMultiColumnDetector detector = new DocxMultiColumnDetector();

	@Test
	void detectsSingleColumnResumeAsNotMultiColumn() throws IOException {
		InputStream docx = docxWithColumnCount(null);

		assertThat(detector.isMultiColumn(docx)).isFalse();
	}

	@Test
	void detectsSectionWithTwoColumnsAsMultiColumn() throws IOException {
		InputStream docx = docxWithColumnCount(2);

		assertThat(detector.isMultiColumn(docx)).isTrue();
	}

	private InputStream docxWithColumnCount(Integer columnCount) throws IOException {
		try (XWPFDocument document = new XWPFDocument()) {
			document.createParagraph().createRun().setText("Ana Silva");
			if (columnCount != null) {
				CTSectPr sectPr = document.getDocument().getBody().addNewSectPr();
				sectPr.addNewCols().setNum(BigInteger.valueOf(columnCount));
			}
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			document.write(out);
			return new ByteArrayInputStream(out.toByteArray());
		}
	}
}
