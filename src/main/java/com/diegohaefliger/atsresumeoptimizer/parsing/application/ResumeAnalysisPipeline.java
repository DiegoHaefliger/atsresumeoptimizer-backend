package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.NormalizedDocument;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingResult;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.ParsingSignals;
import com.diegohaefliger.atsresumeoptimizer.parsing.domain.SourceFormat;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.springframework.stereotype.Component;

@Component
public class ResumeAnalysisPipeline {

	private final DocumentMimeValidator mimeValidator;
	private final SectionSegmenter sectionSegmenter;
	private final ContactExtractor contactExtractor;
	private final IconGlyphDetector iconGlyphDetector;
	private final NoTextLayerDetector noTextLayerDetector;

	ResumeAnalysisPipeline(
			DocumentMimeValidator mimeValidator,
			SectionSegmenter sectionSegmenter,
			ContactExtractor contactExtractor,
			IconGlyphDetector iconGlyphDetector,
			NoTextLayerDetector noTextLayerDetector) {
		this.mimeValidator = mimeValidator;
		this.sectionSegmenter = sectionSegmenter;
		this.contactExtractor = contactExtractor;
		this.iconGlyphDetector = iconGlyphDetector;
		this.noTextLayerDetector = noTextLayerDetector;
	}

	public ParsingResult analyze(byte[] content) {
		SourceFormat format = mimeValidator.detectSupportedFormat(content);
		FormatInspectors inspectors = FormatInspectors.of(format);
		NormalizedDocument document = inspectors.extractor().extract(stream(content));

		ParsingSignals signals = new ParsingSignals(
				format == SourceFormat.PDF && noTextLayerDetector.hasNoTextLayer(stream(content)),
				inspectors.multiColumn().isMultiColumn(stream(content)),
				inspectors.table().hasTable(stream(content)),
				inspectors.headerFooterContact().hasContactInHeaderOrFooter(stream(content)),
				inspectors.textBox().hasTextBox(stream(content)),
				iconGlyphDetector.hasIconGlyphsWithoutUnicode(document.rawText()),
				inspectors.hiddenText().hasHiddenText(stream(content)),
				inspectors.fontConsistency().hasInconsistentFonts(stream(content)));

		return new ParsingResult(document, signals, sectionSegmenter.segment(document.rawText()),
				contactExtractor.extract(document.rawText()));
	}

	private static InputStream stream(byte[] content) {
		return new ByteArrayInputStream(content);
	}
}
