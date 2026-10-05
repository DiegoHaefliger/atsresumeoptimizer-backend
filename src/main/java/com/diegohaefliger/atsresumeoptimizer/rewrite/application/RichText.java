package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import com.diegohaefliger.atsresumeoptimizer.ai.TextSpan;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Mesma sintaxe do editor: **negrito**, *itálico*, ++sublinhado++, "- " e "1. " no início da linha. */
final class RichText {

	record Line(String marker, List<TextSpan> spans) {

		boolean isBlank() {
			return marker.isEmpty() && TextSpan.plainText(spans).isBlank();
		}
	}

	private static final Pattern LIST_MARKER = Pattern.compile("^(- |\\d+\\. )");
	private static final String BULLET_MARKER = "• ";
	private static final String BOLD = "**";
	private static final String UNDERLINE = "++";
	private static final String ITALIC = "*";

	private RichText() {
	}

	static List<Line> fromMarkup(String markup) {
		if (markup == null) {
			return List.of(new Line("", List.of(new TextSpan("", false))));
		}
		List<Line> lines = new ArrayList<>();
		for (String raw : markup.strip().split("\\R", -1)) {
			Matcher marker = LIST_MARKER.matcher(raw.stripLeading());
			if (marker.find()) {
				lines.add(new Line(displayMarker(marker.group(1)), inline(raw.stripLeading().substring(marker.end()))));
			} else {
				lines.add(new Line("", inline(raw)));
			}
		}
		return lines;
	}

	static Line fromSpans(List<TextSpan> spans) {
		if (spans.isEmpty()) {
			return new Line("", List.of(new TextSpan("", false)));
		}
		TextSpan first = spans.get(0);
		String firstText = first.text() == null ? "" : first.text().stripLeading();
		Matcher marker = LIST_MARKER.matcher(firstText);
		if (!marker.find()) {
			return new Line("", spans);
		}
		List<TextSpan> rest = new ArrayList<>(spans);
		rest.set(0, first.withText(firstText.substring(marker.end())));
		return new Line(displayMarker(marker.group(1)), rest);
	}

	static List<TextSpan> inline(String text) {
		List<TextSpan> spans = new ArrayList<>();
		StringBuilder buffer = new StringBuilder();
		boolean bold = false;
		boolean italic = false;
		boolean underline = false;
		int position = 0;
		while (position < text.length()) {
			String token = tokenAt(text, position);
			boolean active = token != null && switch (token) {
				case BOLD -> bold;
				case UNDERLINE -> underline;
				default -> italic;
			};
			if (token != null && canToggle(text, position, token, active)) {
				flush(spans, buffer, bold, italic, underline);
				switch (token) {
					case BOLD -> bold = !bold;
					case UNDERLINE -> underline = !underline;
					default -> italic = !italic;
				}
				position += token.length();
				continue;
			}
			buffer.append(text.charAt(position));
			position++;
		}
		flush(spans, buffer, bold, italic, underline);
		if (spans.isEmpty()) {
			spans.add(new TextSpan("", false));
		}
		return spans;
	}

	private static String tokenAt(String text, int position) {
		if (text.startsWith(BOLD, position)) {
			return BOLD;
		}
		if (text.startsWith(UNDERLINE, position)) {
			return UNDERLINE;
		}
		return text.startsWith(ITALIC, position) ? ITALIC : null;
	}

	private static boolean canToggle(String text, int position, String token, boolean active) {
		if (active) {
			return position > 0 && !Character.isWhitespace(text.charAt(position - 1));
		}
		int next = position + token.length();
		if (next >= text.length() || Character.isWhitespace(text.charAt(next))) {
			return false;
		}
		for (int close = text.indexOf(token, next); close >= 0; close = text.indexOf(token, close + 1)) {
			if (close > next && !Character.isWhitespace(text.charAt(close - 1))) {
				return true;
			}
		}
		return false;
	}

	private static void flush(List<TextSpan> spans, StringBuilder buffer, boolean bold, boolean italic, boolean underline) {
		if (buffer.length() > 0) {
			spans.add(new TextSpan(buffer.toString(), bold, italic, underline));
			buffer.setLength(0);
		}
	}

	private static String displayMarker(String raw) {
		return raw.equals("- ") ? BULLET_MARKER : raw;
	}
}
