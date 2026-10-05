package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Só número/métrica inventada: reconhecer empresa ou cargo por heurística dá falso positivo demais. */
final class FactGroundingChecker {

	private static final Pattern NUMBER = Pattern.compile("\\d[\\d.,%]*");

	private FactGroundingChecker() {
	}

	static boolean introducesNewNumbers(String original, String rewritten) {
		return numbersIn(rewritten).stream().anyMatch(number -> !numbersIn(original).contains(number));
	}

	private static Set<String> numbersIn(String text) {
		Set<String> numbers = new HashSet<>();
		Matcher matcher = NUMBER.matcher(text);
		while (matcher.find()) {
			numbers.add(matcher.group());
		}
		return numbers;
	}
}
