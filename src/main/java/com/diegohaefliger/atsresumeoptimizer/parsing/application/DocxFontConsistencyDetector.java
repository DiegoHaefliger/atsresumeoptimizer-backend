package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.DocumentExtractionException;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;

class DocxFontConsistencyDetector implements FontConsistencyDetector {

	private static final int MAX_FONT_FAMILIES = 3;
	private static final double MIN_FONT_SIZE_PT = 9.0;

	@Override
	public boolean hasInconsistentFonts(InputStream input) {
		try (XWPFDocument document = new XWPFDocument(input)) {
			List<XWPFRun> runs = collectRuns(document);
			Set<String> families = runs.stream().map(XWPFRun::getFontFamily).filter(Objects::nonNull).collect(Collectors.toSet());
			boolean tooManyFamilies = families.size() > MAX_FONT_FAMILIES;
			boolean hasTinyFont = runs.stream()
				.map(XWPFRun::getFontSizeAsDouble)
				.filter(Objects::nonNull)
				.anyMatch(size -> size < MIN_FONT_SIZE_PT);
			return tooManyFamilies || hasTinyFont;
		} catch (IOException exception) {
			throw new DocumentExtractionException(SourceFormat.DOCX, exception);
		}
	}

	private List<XWPFRun> collectRuns(XWPFDocument document) {
		List<XWPFRun> runs = new ArrayList<>();
		for (IBodyElement element : document.getBodyElements()) {
			if (element instanceof XWPFParagraph paragraph) {
				runs.addAll(paragraph.getRuns());
			} else if (element instanceof XWPFTable table) {
				table.getRows().forEach(row -> row.getTableCells()
					.forEach(cell -> cell.getParagraphs().forEach(paragraph -> runs.addAll(paragraph.getRuns()))));
			}
		}
		return runs;
	}
}
