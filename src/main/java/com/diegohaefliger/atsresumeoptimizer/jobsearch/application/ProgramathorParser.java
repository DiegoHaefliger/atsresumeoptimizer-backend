package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
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
import tools.jackson.databind.JsonNode;

final class ProgramathorParser {

	private static final Pattern JOB_PATH = Pattern.compile("^/jobs/(\\d+-[^/?#]+)");
	private static final String JOB_POSTING = "JobPosting";

	private ProgramathorParser() {
	}

	static List<SourceJob> parseList(String html, String baseUrl) {
		List<SourceJob> jobs = new ArrayList<>();
		for (Element card : Jsoup.parse(html).select("div.cell-list")) {
			Matcher path = JOB_PATH.matcher(card.select("a[href^=/jobs/]").attr("href"));
			Element title = card.selectFirst("h3");
			if (path.find() && title != null && !title.text().isBlank()) {
				jobs.add(new SourceJob(path.group(1), baseUrl + "/jobs/" + path.group(1), title.text().strip(),
						field(card, "fa-briefcase"), field(card, "fa-map-marker-alt"), null));
			}
		}
		return jobs;
	}

	static Optional<SourceJobDetail> parseDetail(String html) {
		Document document = Jsoup.parse(html);
		for (Element script : document.select("script[type=application/ld+json]")) {
			Optional<JsonNode> posting = TolerantJson.parse(script.data()).filter(node -> JOB_POSTING.equals(node.path("@type").asString("")));
			if (posting.isPresent()) {
				return Optional.of(detail(posting.get()));
			}
		}
		return Optional.empty();
	}

	private static SourceJobDetail detail(JsonNode posting) {
		String description = HtmlText.plain(Jsoup.parse(posting.path("description").asString("")).body());
		return new SourceJobDetail(description.isBlank() ? "Sem descrição." : description, null, null,
				"TELECOMMUTE".equals(posting.path("jobLocationType").asString("")) ? WorkModel.REMOTE : null,
				contractType(posting.path("employmentType").asString("")), date(posting.path("datePosted").asString("")), JobPostingLd.salary(posting));
	}

	private static String field(Element card, String iconClass) {
		Element icon = card.selectFirst("i." + iconClass);
		return icon == null || icon.parent() == null ? "" : icon.parent().text().strip();
	}

	private static ContractType contractType(String value) {
		return switch (value) {
			case "CONTRACTOR" -> ContractType.PJ;
			case "FULL_TIME" -> ContractType.CLT;
			case "INTERN" -> ContractType.INTERNSHIP;
			case "TEMPORARY" -> ContractType.TEMPORARY;
			default -> null;
		};
	}

	private static LocalDate date(String value) {
		try {
			return value.isBlank() ? null : LocalDate.parse(value);
		} catch (DateTimeParseException exception) {
			return null;
		}
	}
}
