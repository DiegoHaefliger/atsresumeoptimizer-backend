package com.diegohaefliger.atsresumeoptimizer;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NormalizedText {

	private static final Pattern DIACRITICS = Pattern.compile("\\p{M}");
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");

	private NormalizedText() {
	}

	public static String of(String text) {
		if (text == null) {
			return "";
		}
		String withoutAccents = DIACRITICS.matcher(Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD))
				.replaceAll("");
		return WHITESPACE.matcher(withoutAccents).replaceAll(" ").strip();
	}

	/** Sem limite de palavra, termo curto casa dentro de outra ("IaC" em "criação", "Java" em "JavaScript"). */
	public static Pattern wordPattern(String normalizedTerm) {
		return Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(normalizedTerm) + "(?![\\p{L}\\p{N}])");
	}

	public static boolean containsWord(String normalizedText, String normalizedTerm) {
		return !normalizedTerm.isBlank() && wordPattern(normalizedTerm).matcher(normalizedText).find();
	}

	public static int countWord(String normalizedText, String normalizedTerm) {
		if (normalizedTerm.isBlank()) {
			return 0;
		}
		Matcher matcher = wordPattern(normalizedTerm).matcher(normalizedText);
		int count = 0;
		while (matcher.find()) {
			count++;
		}
		return count;
	}
}
