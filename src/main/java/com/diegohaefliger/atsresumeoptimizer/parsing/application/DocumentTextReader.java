package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.io.ByteArrayInputStream;
import org.springframework.stereotype.Component;

/** Texto cru de PDF, DOCX ou ODT, sem análise de layout; tipo desconhecido lança {@code UnsupportedDocumentTypeException}. */
@Component
public class DocumentTextReader {

	private final DocumentMimeValidator mimeValidator;

	DocumentTextReader(DocumentMimeValidator mimeValidator) {
		this.mimeValidator = mimeValidator;
	}

	public String read(byte[] content) {
		SourceFormat format = mimeValidator.detectSupportedFormat(content);
		return FormatInspectors.of(format).extractor().extract(new ByteArrayInputStream(content)).rawText();
	}
}
