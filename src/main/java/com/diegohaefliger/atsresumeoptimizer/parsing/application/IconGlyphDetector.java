package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import org.springframework.stereotype.Component;

/** Ícone de fonte (Wingdings, Font Awesome) cai na área de uso privado do Unicode e vira lixo no ATS. */
@Component
public class IconGlyphDetector {

	private static final int PRIVATE_USE_AREA_START = 0xE000;
	private static final int PRIVATE_USE_AREA_END = 0xF8FF;

	public boolean hasIconGlyphsWithoutUnicode(String text) {
		return text.codePoints().anyMatch(this::isPrivateUseArea);
	}

	private boolean isPrivateUseArea(int codePoint) {
		return codePoint >= PRIVATE_USE_AREA_START && codePoint <= PRIVATE_USE_AREA_END;
	}
}
