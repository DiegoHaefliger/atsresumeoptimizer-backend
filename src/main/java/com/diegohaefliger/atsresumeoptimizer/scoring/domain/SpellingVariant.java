package com.diegohaefliger.atsresumeoptimizer.scoring.domain;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/** "microserviços" e "microsserviços": mesma palavra com uma letra de diferença, só em palavra longa o bastante. */
public final class SpellingVariant {

	private static final int MIN_WORD_LENGTH = 7;
	private static final int MAX_EDITS = 1;
	private static final Pattern WORD_SEPARATOR = Pattern.compile("[^\\p{L}\\p{N}]+");

	private SpellingVariant() {
	}

	public static boolean same(String left, String right) {
		List<String> leftWords = words(left);
		List<String> rightWords = words(right);
		if (leftWords.isEmpty() || leftWords.size() != rightWords.size()) {
			return false;
		}
		for (int i = 0; i < leftWords.size(); i++) {
			if (!sameWord(leftWords.get(i), rightWords.get(i))) {
				return false;
			}
		}
		return true;
	}

	public static boolean containedIn(String text, String term) {
		List<String> termWords = words(term);
		List<String> textWords = words(text);
		if (termWords.isEmpty()) {
			return false;
		}
		for (int start = 0; start + termWords.size() <= textWords.size(); start++) {
			boolean matches = true;
			for (int i = 0; i < termWords.size() && matches; i++) {
				matches = sameWord(termWords.get(i), textWords.get(start + i));
			}
			if (matches) {
				return true;
			}
		}
		return false;
	}

	public static List<String> words(String text) {
		if (text == null) {
			return List.of();
		}
		return Arrays.stream(WORD_SEPARATOR.split(NormalizedText.of(text))).filter(word -> !word.isEmpty()).toList();
	}

	private static boolean sameWord(String left, String right) {
		if (left.equals(right)) {
			return true;
		}
		return Math.min(left.length(), right.length()) >= MIN_WORD_LENGTH && withinOneEdit(left, right);
	}

	private static boolean withinOneEdit(String left, String right) {
		if (Math.abs(left.length() - right.length()) > MAX_EDITS) {
			return false;
		}
		int i = 0;
		int j = 0;
		int edits = 0;
		while (i < left.length() && j < right.length()) {
			if (left.charAt(i) == right.charAt(j)) {
				i++;
				j++;
				continue;
			}
			if (++edits > MAX_EDITS) {
				return false;
			}
			if (left.length() > right.length()) {
				i++;
			} else if (left.length() < right.length()) {
				j++;
			} else {
				i++;
				j++;
			}
		}
		return edits + (left.length() - i) + (right.length() - j) <= MAX_EDITS;
	}
}
