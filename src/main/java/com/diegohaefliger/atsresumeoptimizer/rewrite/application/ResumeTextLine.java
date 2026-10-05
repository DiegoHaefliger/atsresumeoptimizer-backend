package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

record ResumeTextLine(String text, boolean bullet) {

	private static final Pattern BULLET_PREFIX = Pattern.compile("^[•▪◦●\\-*–]\\s*");
	private static final Pattern SENTENCE_END = Pattern.compile("[.!?:;]$");
	private static final String LIST_CONTINUATION = ",";

	/** O PDF quebra a frase no fim da linha visual; sem juntar, cada pedaço vira um item no editor. */
	static List<ResumeTextLine> of(String content) {
		List<ResumeTextLine> lines = new ArrayList<>();
		for (String raw : content.split("\\R")) {
			String stripped = raw.strip();
			if (stripped.isEmpty()) {
				continue;
			}
			boolean bullet = BULLET_PREFIX.matcher(stripped).find();
			String text = BULLET_PREFIX.matcher(stripped).replaceFirst("");
			if (!bullet && !lines.isEmpty() && continuesPrevious(lines.getLast().text(), text)) {
				ResumeTextLine previous = lines.removeLast();
				lines.add(new ResumeTextLine(previous.text() + " " + text, previous.bullet()));
			} else {
				lines.add(new ResumeTextLine(text, bullet));
			}
		}
		return lines;
	}

	private static boolean continuesPrevious(String previous, String line) {
		return previous.endsWith(LIST_CONTINUATION)
				|| (!SENTENCE_END.matcher(previous).find() && Character.isLowerCase(line.codePointAt(0)));
	}
}
