package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.DocumentExtractionException;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

class PdfFontConsistencyDetector implements FontConsistencyDetector {

	private static final int MAX_FONT_FAMILIES = 3;
	private static final float MIN_FONT_SIZE_PT = 9f;

	@Override
	public boolean hasInconsistentFonts(InputStream input) {
		try (PDDocument document = Loader.loadPDF(input.readAllBytes())) {
			Set<String> fontNames = new HashSet<>();
			AtomicBoolean hasTinyFont = new AtomicBoolean(false);
			PDFTextStripper stripper = new PDFTextStripper() {
				@Override
				protected void writeString(String text, List<TextPosition> textPositions) {
					for (TextPosition position : textPositions) {
						if (position.getUnicode() == null || position.getUnicode().isBlank()) {
							continue;
						}
						fontNames.add(position.getFont().getName());
						if (position.getFontSizeInPt() < MIN_FONT_SIZE_PT) {
							hasTinyFont.set(true);
						}
					}
				}
			};
			stripper.getText(document);
			return fontNames.size() > MAX_FONT_FAMILIES || hasTinyFont.get();
		} catch (IOException exception) {
			throw new DocumentExtractionException(SourceFormat.PDF, exception);
		}
	}
}
