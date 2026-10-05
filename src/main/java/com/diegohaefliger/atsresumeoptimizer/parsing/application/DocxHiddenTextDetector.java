package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.DocumentExtractionException;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;

class DocxHiddenTextDetector implements HiddenTextDetector {

	private static final String WHITE_HEX_COLOR = "FFFFFF";

	@Override
	public boolean hasHiddenText(InputStream input) {
		try (XWPFDocument document = new XWPFDocument(input)) {
			return document.getBodyElements().stream().anyMatch(this::hasHiddenRun);
		} catch (IOException exception) {
			throw new DocumentExtractionException(SourceFormat.DOCX, exception);
		}
	}

	private boolean hasHiddenRun(IBodyElement element) {
		if (element instanceof XWPFParagraph paragraph) {
			return paragraph.getRuns().stream().anyMatch(this::isHidden);
		}
		if (element instanceof XWPFTable table) {
			return table.getRows().stream()
				.flatMap(row -> row.getTableCells().stream())
				.flatMap(cell -> cell.getParagraphs().stream())
				.flatMap(paragraph -> paragraph.getRuns().stream())
				.anyMatch(this::isHidden);
		}
		return false;
	}

	private boolean isHidden(XWPFRun run) {
		String text = run.getText(0);
		if (text == null || text.isBlank()) {
			return false;
		}
		return run.isVanish() || WHITE_HEX_COLOR.equalsIgnoreCase(run.getColor());
	}
}
