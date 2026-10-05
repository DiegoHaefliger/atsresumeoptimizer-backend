package com.diegohaefliger.atsresumeoptimizer.ai;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import java.util.List;
import java.util.stream.Collectors;

/** A IA costuma omitir os flags em span sem estilo; Jackson 3 falharia no primitivo ausente. */
public record TextSpan(
		String text,
		@JsonSetter(nulls = Nulls.AS_EMPTY) boolean bold,
		@JsonSetter(nulls = Nulls.AS_EMPTY) boolean italic,
		@JsonSetter(nulls = Nulls.AS_EMPTY) boolean underline) {

	public TextSpan(String text, boolean bold) {
		this(text, bold, false, false);
	}

	public TextSpan withText(String newText) {
		return new TextSpan(newText, bold, italic, underline);
	}

	public static String plainText(List<TextSpan> spans) {
		return spans.stream().map(TextSpan::text).collect(Collectors.joining());
	}
}
