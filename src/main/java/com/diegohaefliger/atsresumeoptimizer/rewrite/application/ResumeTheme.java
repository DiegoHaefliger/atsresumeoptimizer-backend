package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.rewrite.domain.ResumeTemplate;
import java.awt.Color;

record ResumeTheme(String accentHex, int nameFontSize, String contactSeparator) {

	private static final String BLACK_HEX = "000000";
	private static final String NAVY_BLUE_HEX = "1F4E79";

	static ResumeTheme of(ResumeTemplate template) {
		return switch (template) {
			case CLASSIC -> new ResumeTheme(BLACK_HEX, 18, " | ");
			case MODERN_BLUE -> new ResumeTheme(NAVY_BLUE_HEX, 20, " | ");
		};
	}

	Color accentColor() {
		return Color.decode("#" + accentHex);
	}
}
