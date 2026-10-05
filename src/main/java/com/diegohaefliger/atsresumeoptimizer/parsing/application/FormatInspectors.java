package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;

record FormatInspectors(
		DocumentExtractor extractor,
		MultiColumnDetector multiColumn,
		TableDetector table,
		HeaderFooterContactDetector headerFooterContact,
		TextBoxDetector textBox,
		HiddenTextDetector hiddenText,
		FontConsistencyDetector fontConsistency) {

	private static final FormatInspectors PDF = new FormatInspectors(new PdfExtractor(), new PdfMultiColumnDetector(),
			new PdfTableDetector(), new PdfHeaderFooterContactDetector(), input -> false, new PdfHiddenTextDetector(),
			new PdfFontConsistencyDetector());

	private static final FormatInspectors DOCX = new FormatInspectors(new DocxExtractor(), new DocxMultiColumnDetector(),
			new DocxTableDetector(), new DocxHeaderFooterContactDetector(), new DocxTextBoxDetector(),
			new DocxHiddenTextDetector(), new DocxFontConsistencyDetector());

	// ODT ainda sem heurística de texto oculto e de fonte: w:vanish e cor de fundo não têm equivalente direto no ODF.
	private static final FormatInspectors ODT = new FormatInspectors(new OdtExtractor(), new OdtMultiColumnDetector(),
			new OdtTableDetector(), new OdtHeaderFooterContactDetector(), new OdtTextBoxDetector(), input -> false,
			input -> false);

	static FormatInspectors of(SourceFormat format) {
		return switch (format) {
			case PDF -> PDF;
			case DOCX -> DOCX;
			case ODT -> ODT;
		};
	}
}
