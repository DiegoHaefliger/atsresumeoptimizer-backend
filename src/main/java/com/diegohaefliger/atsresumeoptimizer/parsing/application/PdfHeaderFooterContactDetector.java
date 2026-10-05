package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.DocumentExtractionException;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

/** Repetição no topo/rodapé só se confirma com duas páginas ou mais. */
class PdfHeaderFooterContactDetector implements HeaderFooterContactDetector {

	private static final float BAND_HEIGHT_POINTS = 60f;

	@Override
	public boolean hasContactInHeaderOrFooter(InputStream input) {
		try (PDDocument document = Loader.loadPDF(input.readAllBytes())) {
			if (document.getNumberOfPages() < 2) {
				return false;
			}
			return bandHasContactInfoOnEveryPage(document, true) || bandHasContactInfoOnEveryPage(document, false);
		} catch (IOException exception) {
			throw new DocumentExtractionException(SourceFormat.PDF, exception);
		}
	}

	private boolean bandHasContactInfoOnEveryPage(PDDocument document, boolean topBand) throws IOException {
		for (int pageIndex = 0; pageIndex < document.getNumberOfPages(); pageIndex++) {
			if (!pageBandHasContactInfo(document, pageIndex, topBand)) {
				return false;
			}
		}
		return true;
	}

	private boolean pageBandHasContactInfo(PDDocument document, int pageIndex, boolean topBand) throws IOException {
		PDPage page = document.getPage(pageIndex);
		float pageHeight = page.getMediaBox().getHeight();
		AtomicBoolean found = new AtomicBoolean(false);
		PDFTextStripper stripper = new PDFTextStripper() {
			@Override
			protected void writeString(String text, List<TextPosition> textPositions) {
				if (text.isBlank() || textPositions.isEmpty()) {
					return;
				}
				float y = textPositions.get(0).getYDirAdj();
				boolean inBand = topBand ? y <= BAND_HEIGHT_POINTS : y >= pageHeight - BAND_HEIGHT_POINTS;
				if (inBand && ContactPatterns.containsContactInfo(text)) {
					found.set(true);
				}
			}
		};
		stripper.setStartPage(pageIndex + 1);
		stripper.setEndPage(pageIndex + 1);
		stripper.getText(document);
		return found.get();
	}
}
