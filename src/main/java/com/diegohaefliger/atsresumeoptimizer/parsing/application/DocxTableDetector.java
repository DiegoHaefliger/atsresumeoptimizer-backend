package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.DocumentExtractionException;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;

class DocxTableDetector implements TableDetector {

	@Override
	public boolean hasTable(InputStream input) {
		try (XWPFDocument document = new XWPFDocument(input)) {
			return document.getTables().stream().anyMatch(this::hasRelevantContent);
		} catch (IOException exception) {
			throw new DocumentExtractionException(SourceFormat.DOCX, exception);
		}
	}

	private boolean hasRelevantContent(XWPFTable table) {
		return !table.getText().isBlank();
	}
}
