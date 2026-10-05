package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.DocumentExtractionException;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

class DocxHeaderFooterContactDetector implements HeaderFooterContactDetector {

	@Override
	public boolean hasContactInHeaderOrFooter(InputStream input) {
		try (XWPFDocument document = new XWPFDocument(input)) {
			return Stream.concat(
				document.getHeaderList().stream().map(header -> header.getText()),
				document.getFooterList().stream().map(footer -> footer.getText())
			).anyMatch(ContactPatterns::containsContactInfo);
		} catch (IOException exception) {
			throw new DocumentExtractionException(SourceFormat.DOCX, exception);
		}
	}
}
