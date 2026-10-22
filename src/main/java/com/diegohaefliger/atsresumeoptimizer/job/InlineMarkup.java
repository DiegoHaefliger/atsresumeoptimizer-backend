package com.diegohaefliger.atsresumeoptimizer.job;

import java.util.regex.Pattern;

/** Marcas de formatação do editor (negrito, sublinhado, itálico) que não devem chegar a comparações nem à IA. */
public final class InlineMarkup {

	private static final Pattern BOLD = Pattern.compile("\\*\\*(.+?)\\*\\*");
	private static final Pattern UNDERLINE = Pattern.compile("\\+\\+(.+?)\\+\\+");
	private static final Pattern ITALIC = Pattern.compile("(?<!\\*)\\*(?!\\s)(.+?)(?<!\\s)\\*(?!\\*)");

	private InlineMarkup() {
	}

	public static String strip(String text) {
		String plain = BOLD.matcher(text).replaceAll("$1");
		plain = UNDERLINE.matcher(plain).replaceAll("$1");
		return ITALIC.matcher(plain).replaceAll("$1");
	}
}
