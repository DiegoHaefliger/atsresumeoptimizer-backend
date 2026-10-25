package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.util.regex.Pattern;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;

final class HtmlText {

	private static final Pattern BLANK_LINES = Pattern.compile("\\n{3,}");
	private static final Pattern PADDING_SPACES = Pattern.compile("(?m)^[ \\t]+|[ \\t]+$");
	private static final Pattern ENTITY = Pattern.compile("&(#\\d+|#[xX][0-9a-fA-F]+|[a-zA-Z]{2,8});");
	private static final char NO_BREAK_SPACE = '\u00A0';

	private HtmlText() {
	}

	static String plain(Element markup) {
		Element copy = markup.clone();
		copy.select("li").before("• ").after("\n");
		copy.select("p, ul, ol, h1, h2, h3, h4, h5, h6").after("\n\n");
		String text = PADDING_SPACES.matcher(copy.wholeText()).replaceAll("");
		return decoded(BLANK_LINES.matcher(text).replaceAll("\n\n").strip());
	}

	static String decoded(String text) {
		String unescaped = ENTITY.matcher(text).find() ? Parser.unescapeEntities(text, false) : text;
		return unescaped.replace(NO_BREAK_SPACE, ' ').strip();
	}
}
