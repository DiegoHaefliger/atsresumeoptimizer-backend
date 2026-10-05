package com.diegohaefliger.atsresumeoptimizer.parsing.domain;

public record ParsingSignals(
		boolean noTextLayer,
		boolean multiColumn,
		boolean hasTable,
		boolean contactInHeaderOrFooter,
		boolean hasTextBox,
		boolean hasIconGlyphsWithoutUnicode,
		boolean hasHiddenText,
		boolean hasInconsistentFonts) {
}
