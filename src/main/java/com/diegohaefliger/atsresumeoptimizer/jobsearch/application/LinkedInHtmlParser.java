package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

final class LinkedInHtmlParser {

	private static final Pattern JOB_URN = Pattern.compile("urn:li:jobPosting:(\\d+)");

	private LinkedInHtmlParser() {
	}

	static List<SourceJob> parseSearch(String html) {
		List<SourceJob> jobs = new ArrayList<>();
		for (Element card : Jsoup.parse(html).select("div.base-search-card[data-entity-urn]")) {
			Matcher urn = JOB_URN.matcher(card.attr("data-entity-urn"));
			String title = text(card, "h3.base-search-card__title");
			if (urn.find() && !title.isEmpty()) {
				jobs.add(new SourceJob(urn.group(1), withoutQuery(card.select("a.base-card__full-link").attr("href")), title,
						text(card, "h4.base-search-card__subtitle"), text(card, "span.job-search-card__location"),
						date(card.select("time[datetime]").attr("datetime"))));
			}
		}
		return jobs;
	}

	static Optional<SourceJobDetail> parseDetail(String html) {
		Document document = Jsoup.parse(html);
		Element markup = document.selectFirst("div.show-more-less-html__markup");
		if (markup == null) {
			return Optional.empty();
		}
		String seniority = null;
		String employment = null;
		for (Element item : document.select("li.description__job-criteria-item")) {
			String header = text(item, "h3.description__job-criteria-subheader").toLowerCase();
			String value = text(item, "span.description__job-criteria-text");
			if (header.contains("seniority") || header.contains("nível")) {
				seniority = value;
			} else if (header.contains("employment") || header.contains("tipo de emprego")) {
				employment = value;
			}
		}
		return Optional.of(new SourceJobDetail(HtmlText.plain(markup), seniority, employment));
	}

	private static String text(Element parent, String selector) {
		Element element = parent.selectFirst(selector);
		return element == null ? "" : element.text().strip();
	}

	private static String withoutQuery(String url) {
		int query = url.indexOf('?');
		return query < 0 ? url : url.substring(0, query);
	}

	private static LocalDate date(String value) {
		try {
			return value.isBlank() ? null : LocalDate.parse(value);
		} catch (DateTimeParseException exception) {
			return null;
		}
	}
}
