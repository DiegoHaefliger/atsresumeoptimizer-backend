package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.DocumentExtractionException;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.io.IOException;
import java.io.InputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

class PdfTableDetector implements TableDetector {

	private static final int MIN_RECTANGLES = 3;
	private static final int MIN_HORIZONTAL_LINES = 2;
	private static final int MIN_VERTICAL_LINES = 3;

	@Override
	public boolean hasTable(InputStream input) {
		try (PDDocument document = Loader.loadPDF(input.readAllBytes())) {
			for (PDPage page : document.getPages()) {
				if (looksLikeTable(page)) {
					return true;
				}
			}
			return false;
		} catch (IOException exception) {
			throw new DocumentExtractionException(SourceFormat.PDF, exception);
		}
	}

	private boolean looksLikeTable(PDPage page) throws IOException {
		PdfLineGridCounter counter = new PdfLineGridCounter(page);
		counter.processPage(page);
		boolean hasGridOfRectangles = counter.rectangles() >= MIN_RECTANGLES;
		boolean hasCrossingRuleLines = counter.horizontalLines() >= MIN_HORIZONTAL_LINES && counter.verticalLines() >= MIN_VERTICAL_LINES;
		return hasGridOfRectangles || hasCrossingRuleLines;
	}
}
