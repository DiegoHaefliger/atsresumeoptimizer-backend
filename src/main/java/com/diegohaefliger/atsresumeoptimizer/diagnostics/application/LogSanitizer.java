package com.diegohaefliger.atsresumeoptimizer.diagnostics.application;

import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
class LogSanitizer {

	private static final String MASK = "***";

	private record Rule(Pattern pattern, String replacement) {
	}

	private static final List<Rule> RULES = List.of(
			new Rule(Pattern.compile("eyJ[\\w-]{5,}\\.[\\w-]{5,}\\.[\\w-]+"), MASK),
			new Rule(Pattern.compile("\\b(sk-ant-|sk-proj-|sk-or-v1-|sk-|gsk_|xai-|AIza|ghp_|github_pat_|sb_secret_)[\\w-]{8,}"), "$1" + MASK),
			new Rule(Pattern.compile("(?i)\\b(bearer|basic)\\s+[\\w.~+/=-]{8,}"), "$1 " + MASK),
			new Rule(Pattern.compile("(?i)([\\w-]*(?:api[_-]?key|token|secret|password|passwd|senha|authorization)\"?\\s*[:=]\\s*\"?)[^\\s\",;&}]+"), "$1" + MASK),
			new Rule(Pattern.compile("(?i)([a-z][a-z0-9+.-]*://[^\\s:/@]+:)[^\\s@/]+@"), "$1" + MASK + "@"),
			new Rule(Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+"), MASK + "@" + MASK));

	String clean(String text) {
		String cleaned = text;
		for (Rule rule : RULES) {
			cleaned = rule.pattern().matcher(cleaned).replaceAll(rule.replacement());
		}
		return cleaned;
	}
}
