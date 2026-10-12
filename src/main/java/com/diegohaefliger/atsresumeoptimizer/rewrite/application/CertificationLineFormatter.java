package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Certificação sai como "Curso — Instituição | Ano" sem negrito, mesmo em currículos salvos com o formato antigo. */
final class CertificationLineFormatter {

	private static final Pattern TITLE_AND_YEAR = Pattern.compile("^(?<title>.+?)\\s*(?:[—–|]|\\s-\\s)\\s*(?<year>\\d{4})\\s*$");

	private CertificationLineFormatter() {
	}

	static List<TextSpan> format(List<TextSpan> line) {
		String text = TextSpan.plainText(line).strip();
		if (text.isEmpty()) {
			return line;
		}
		if (line.stream().anyMatch(TextSpan::bold)) {
			return line.stream().map(span -> new TextSpan(span.text(), false, span.italic(), span.underline())).toList();
		}
		Matcher matcher = TITLE_AND_YEAR.matcher(text);
		if (matcher.matches()) {
			return List.of(new TextSpan(matcher.group("title").strip(), false),
					new TextSpan(" | " + matcher.group("year"), false));
		}
		return List.of(new TextSpan(text, false));
	}
}
