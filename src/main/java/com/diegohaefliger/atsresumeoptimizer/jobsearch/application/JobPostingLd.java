package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import tools.jackson.databind.JsonNode;

final class JobPostingLd {

	private static final String JOB_POSTING = "JobPosting";
	private static final String MONTHLY = "MONTH";
	private static final int ISO_DATE_LENGTH = 10;

	private JobPostingLd() {
	}

	static Optional<JsonNode> find(Document document) {
		for (Element script : document.select("script[type=application/ld+json]")) {
			Optional<JsonNode> posting = TolerantJson.parse(script.data())
					.filter(node -> JOB_POSTING.equals(node.path("@type").asString("")));
			if (posting.isPresent()) {
				return posting;
			}
		}
		return Optional.empty();
	}

	static String description(JsonNode posting) {
		return HtmlText.plain(Jsoup.parse(posting.path("description").asString("")).body());
	}

	static SalaryRange salary(JsonNode posting) {
		JsonNode value = posting.path("baseSalary").path("value");
		if (!MONTHLY.equalsIgnoreCase(value.path("unitText").asString(""))) {
			return SalaryRange.NONE;
		}
		BigDecimal min = amount(value.path("minValue"));
		BigDecimal max = amount(value.path("maxValue"));
		BigDecimal single = amount(value.path("value"));
		return new SalaryRange(min != null ? min : single, max != null ? max : single);
	}

	private static BigDecimal amount(JsonNode node) {
		return node.isNumber() && node.decimalValue().signum() > 0 ? node.decimalValue() : null;
	}

	static LocalDate postedOn(JsonNode posting) {
		String value = posting.path("datePosted").asString("");
		try {
			return value.length() < ISO_DATE_LENGTH ? null : LocalDate.parse(value.substring(0, ISO_DATE_LENGTH));
		} catch (DateTimeParseException exception) {
			return null;
		}
	}
}
