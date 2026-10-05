package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.UnsupportedDocumentTypeException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;

/** Sem os parsers do Tika, OOXML genérico só se confirma DOCX (e não XLSX/PPTX) pela entrada "word/document.xml". */
@Component
public class DocumentMimeValidator {

	private static final String PDF_MIME_TYPE = "application/pdf";
	private static final String DOCX_MIME_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	private static final String OOXML_ZIP_MIME_TYPE = "application/x-tika-ooxml";
	private static final String DOCX_ZIP_ENTRY = "word/document.xml";
	private static final String ODT_MIME_TYPE = "application/vnd.oasis.opendocument.text";

	private final Tika tika = new Tika();

	public SourceFormat detectSupportedFormat(byte[] content) {
		String mimeType = tika.detect(content);
		if (PDF_MIME_TYPE.equals(mimeType)) {
			return SourceFormat.PDF;
		}
		if (isDocx(mimeType, content)) {
			return SourceFormat.DOCX;
		}
		if (ODT_MIME_TYPE.equals(mimeType)) {
			return SourceFormat.ODT;
		}
		throw new UnsupportedDocumentTypeException(mimeType);
	}

	private boolean isDocx(String mimeType, byte[] content) {
		if (!DOCX_MIME_TYPE.equals(mimeType) && !OOXML_ZIP_MIME_TYPE.equals(mimeType)) {
			return false;
		}
		try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
			for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
				if (DOCX_ZIP_ENTRY.equals(entry.getName())) {
					return true;
				}
			}
		} catch (IOException exception) {
			return false;
		}
		return false;
	}
}
