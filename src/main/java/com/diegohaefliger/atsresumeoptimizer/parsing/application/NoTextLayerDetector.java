package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.DocumentExtractionException;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.io.IOException;
import java.io.InputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

@Component
public class NoTextLayerDetector {

	private static final int MIN_MEANINGFUL_CHARS_PER_PAGE = 50;

	public boolean hasNoTextLayer(InputStream input) {
		try (PDDocument document = Loader.loadPDF(input.readAllBytes())) {
			int pageCount = document.getNumberOfPages();
			if (pageCount == 0) {
				return true;
			}
			long meaningfulChars = countMeaningfulChars(new PDFTextStripper().getText(document));
			return meaningfulChars < (long) MIN_MEANINGFUL_CHARS_PER_PAGE * pageCount;
		} catch (IOException exception) {
			throw new DocumentExtractionException(SourceFormat.PDF, exception);
		}
	}

	private long countMeaningfulChars(String text) {
		return text.chars().filter(character -> !Character.isWhitespace(character)).count();
	}
}
