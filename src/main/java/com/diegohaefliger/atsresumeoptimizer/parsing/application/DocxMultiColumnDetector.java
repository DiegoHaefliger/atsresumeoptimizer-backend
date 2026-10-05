package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.DocumentExtractionException;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.io.IOException;
import java.io.InputStream;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;

class DocxMultiColumnDetector implements MultiColumnDetector {

	@Override
	public boolean isMultiColumn(InputStream input) {
		try (XWPFDocument document = new XWPFDocument(input)) {
			return columnCount(document) > 1;
		} catch (IOException exception) {
			throw new DocumentExtractionException(SourceFormat.DOCX, exception);
		}
	}

	private int columnCount(XWPFDocument document) {
		CTSectPr sectPr = document.getDocument().getBody().getSectPr();
		if (sectPr == null || !sectPr.isSetCols() || sectPr.getCols().getNum() == null) {
			return 1;
		}
		return sectPr.getCols().getNum().intValue();
	}
}
