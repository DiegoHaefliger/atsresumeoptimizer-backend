package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import java.util.Set;
import java.util.regex.Pattern;

/** LanguageTool cobra meia-risca colada antes da UF ("Panambi–RS"); vírgula e hífen disparam o mesmo achado. */
final class GrammarSanitizer {

	private static final Pattern MULTIPLE_SPACES = Pattern.compile("[ \\t]{2,}");

	private static final Set<String> BRAZILIAN_STATE_CODES = Set.of(
			"AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE",
			"PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO");

	private static final Pattern SEPARATOR_BEFORE_STATE_CODE =
			Pattern.compile("\\s*[,-]\\s*(" + String.join("|", BRAZILIAN_STATE_CODES) + ")\\b");

	private GrammarSanitizer() {
	}

	static String sanitize(String text) {
		if (text == null || text.isBlank()) {
			return text;
		}
		String withoutDoubleSpaces = MULTIPLE_SPACES.matcher(text).replaceAll(" ");
		return SEPARATOR_BEFORE_STATE_CODE.matcher(withoutDoubleSpaces).replaceAll("–$1");
	}
}
