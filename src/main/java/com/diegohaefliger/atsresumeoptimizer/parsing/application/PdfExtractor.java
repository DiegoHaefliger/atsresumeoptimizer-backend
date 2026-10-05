package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.DocumentExtractionException;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.NormalizedDocument;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.io.IOException;
import java.io.InputStream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

class PdfExtractor implements DocumentExtractor {

	@Override
	public NormalizedDocument extract(InputStream input) {
		try (PDDocument document = Loader.loadPDF(input.readAllBytes())) {
			return new NormalizedDocument(
					SourceFormat.PDF, stripText(document, false), stripText(document, true), document.getNumberOfPages());
		} catch (IOException exception) {
			throw new DocumentExtractionException(SourceFormat.PDF, exception);
		}
	}

	private String stripText(PDDocument document, boolean sortByPosition) throws IOException {
		PDFTextStripper stripper = new PDFTextStripper();
		stripper.setSortByPosition(sortByPosition);
		return stripper.getText(document);
	}
}
