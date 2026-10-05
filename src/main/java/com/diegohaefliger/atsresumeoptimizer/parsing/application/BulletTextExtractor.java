package com.diegohaefliger.atsresumeoptimizer.parsing.application;

import com.diegohaefliger.atsresumeoptimizer.parsing.domain.Section;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/** Linha sem marcador logo após um bullet que não terminou em pontuação final é continuação quebrada pelo PDF. */
@Component
public class BulletTextExtractor {

	private static final Pattern BULLET_LINE_START = Pattern.compile("^[•‣◦●▪○\\-*]\\s+|^\\d+[.)]\\s+");

	/** Word/Google Docs põem zero-width space entre o marcador e o espaço, e aí {@code \s+} não casa. */
	private static final Pattern INVISIBLE_CHARS = Pattern.compile("[\\u200B\\u200C\\u200D\\uFEFF\\u2060]");

	public List<String> extract(List<Section> sections) {
		return extractGroups(sections).stream().flatMap(List::stream).toList();
	}

	public List<List<String>> extractGroups(List<Section> sections) {
		List<List<String>> groups = new ArrayList<>();
		for (Section section : sections) {
			if (!section.recognized()) {
				continue;
			}
			List<String> group = new ArrayList<>();
			StringBuilder current = null;
			for (String rawLine : section.content().lines().toList()) {
				String line = INVISIBLE_CHARS.matcher(rawLine).replaceAll("").strip();
				if (line.isBlank()) {
					continue;
				}
				if (BULLET_LINE_START.matcher(line).find()) {
					if (current != null) {
						group.add(current.toString());
					}
					current = new StringBuilder(line);
				} else if (current != null && !endsWithSentenceTerminator(current)) {
					current.append(' ').append(line);
				} else {
					if (current != null) {
						group.add(current.toString());
						current = null;
					}
					if (!group.isEmpty()) {
						groups.add(group);
						group = new ArrayList<>();
					}
				}
			}
			if (current != null) {
				group.add(current.toString());
			}
			if (!group.isEmpty()) {
				groups.add(group);
			}
		}
		return groups;
	}

	public String stripMarker(String line) {
		return BULLET_LINE_START.matcher(line).replaceFirst("");
	}

	private boolean endsWithSentenceTerminator(StringBuilder text) {
		if (text.isEmpty()) {
			return false;
		}
		char last = text.charAt(text.length() - 1);
		return last == '.' || last == '!' || last == '?';
	}
}
