package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.DocumentExtractionException;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.xmlbeans.XmlCursor;

/** Caixa de texto legada ou DrawingML sempre encapsula o conteúdo em {@code w:txbxContent}. */
class DocxTextBoxDetector implements TextBoxDetector {

	private static final String WORDPROCESSINGML_NAMESPACE = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";

	@Override
	public boolean hasTextBox(InputStream input) {
		try (XWPFDocument document = new XWPFDocument(input)) {
			try (XmlCursor cursor = document.getDocument().newCursor()) {
				cursor.selectPath("declare namespace w='" + WORDPROCESSINGML_NAMESPACE + "' .//w:txbxContent");
				return cursor.getSelectionCount() > 0;
			}
		} catch (IOException exception) {
			throw new DocumentExtractionException(SourceFormat.DOCX, exception);
		}
	}
}
