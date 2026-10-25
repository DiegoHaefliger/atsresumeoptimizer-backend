package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.util.Optional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

final class LinkedInPostParser {

	private static final String SEGMENTS = "p.attributed-text-segment-list__content";
	private static final String OG_DESCRIPTION = "meta[property=og:description]";
	private static final String AUTH_WALL = "authwall";

	private LinkedInPostParser() {
	}

	static Optional<String> parse(String html) {
		if (html == null || html.isBlank() || html.contains(AUTH_WALL)) {
			return Optional.empty();
		}
		Document document = Jsoup.parse(html);
		Elements segments = document.select(SEGMENTS);
		String text = segments.isEmpty() ? ogDescription(document) : segmentsText(segments);
		return text.isBlank() ? Optional.empty() : Optional.of(text);
	}

	private static String segmentsText(Elements segments) {
		return String.join("\n\n", segments.stream().map(Element::wholeText).map(String::strip).filter(s -> !s.isEmpty()).toList());
	}

	private static String ogDescription(Document document) {
		Element meta = document.selectFirst(OG_DESCRIPTION);
		return meta == null ? "" : meta.attr("content").strip();
	}
}
