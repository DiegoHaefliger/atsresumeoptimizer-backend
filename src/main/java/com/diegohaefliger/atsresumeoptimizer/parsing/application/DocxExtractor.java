package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.DocumentExtractionException;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.NormalizedDocument;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

class DocxExtractor implements DocumentExtractor {

	@Override
	public NormalizedDocument extract(InputStream input) {
		try (XWPFDocument document = new XWPFDocument(input)) {
			return new NormalizedDocument(SourceFormat.DOCX, extractRawText(document), extractStructuredText(document), null);
		} catch (IOException exception) {
			throw new DocumentExtractionException(SourceFormat.DOCX, exception);
		}
	}

	private String extractRawText(XWPFDocument document) throws IOException {
		try (XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
			return extractor.getText();
		}
	}

	private String extractStructuredText(XWPFDocument document) {
		StringBuilder builder = new StringBuilder();
		for (IBodyElement element : document.getBodyElements()) {
			if (element instanceof XWPFParagraph paragraph) {
				builder.append(paragraph.getText()).append(System.lineSeparator());
			} else if (element instanceof XWPFTable table) {
				appendTable(builder, table);
			}
		}
		return builder.toString();
	}

	private void appendTable(StringBuilder builder, XWPFTable table) {
		for (XWPFTableRow row : table.getRows()) {
			String rowText = row.getTableCells().stream().map(XWPFTableCell::getText).collect(Collectors.joining(" | "));
			builder.append(rowText).append(System.lineSeparator());
		}
	}
}
