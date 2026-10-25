package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

final class VagasParser {

	private static final Pattern JOB_PATH = Pattern.compile("^/vagas/v(\\d+/[^/?#]+)");
	private static final DateTimeFormatter DAY_MONTH_YEAR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private VagasParser() {
	}

	static List<SourceJob> parseList(String html, String baseUrl) {
		List<SourceJob> jobs = new ArrayList<>();
		for (Element card : Jsoup.parse(html).select("li.vaga")) {
			Element link = card.selectFirst("a.link-detalhes-vaga");
			Matcher path = link == null ? null : JOB_PATH.matcher(link.attr("href"));
			if (path != null && path.find()) {
				String title = link.hasAttr("title") ? link.attr("title") : link.text();
				jobs.add(new SourceJob(path.group(1), baseUrl + "/vagas/v" + path.group(1), title.strip(),
						text(card, ".emprVaga"), text(card, ".vaga-local"), postedOn(text(card, ".data-publicacao"))));
			}
		}
		return jobs;
	}

	static Optional<SourceJobDetail> parseDetail(String html) {
		return JobPostingLd.find(Jsoup.parse(html)).map(posting -> {
			String description = JobPostingLd.description(posting);
			String benefits = posting.path("jobBenefits").asString("");
			String full = benefits.isBlank() ? description : description + "\n\nBenefícios: " + benefits;
			return new SourceJobDetail(full.isBlank() ? "Sem descrição." : full, null, null, null, null,
					JobPostingLd.postedOn(posting), JobPostingLd.salary(posting));
		});
	}

	private static String text(Element card, String selector) {
		Element element = card.selectFirst(selector);
		return element == null ? "" : element.text().strip();
	}

	private static LocalDate postedOn(String value) {
		try {
			return value.isBlank() ? null : LocalDate.parse(value, DAY_MONTH_YEAR);
		} catch (DateTimeParseException exception) {
			return null;
		}
	}
}
