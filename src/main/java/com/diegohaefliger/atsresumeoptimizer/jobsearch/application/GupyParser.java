package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import tools.jackson.databind.JsonNode;

final class GupyParser {

	private static final String NEXT_DATA = "script#__NEXT_DATA__";

	private GupyParser() {
	}

	static List<SourceJob> parse(String html) {
		Element script = Jsoup.parse(html).selectFirst(NEXT_DATA);
		if (script == null) {
			return List.of();
		}
		JsonNode data = TolerantJson.parse(script.data()).map(root -> root.path("props").path("pageProps")
				.path("initialJobList").path("data")).orElse(null);
		List<SourceJob> jobs = new ArrayList<>();
		if (data == null || !data.isArray()) {
			return jobs;
		}
		for (JsonNode node : data) {
			String id = node.path("id").asString("");
			String title = node.path("name").asString("").strip();
			String url = node.path("jobUrl").asString("");
			if (id.isBlank() || title.isEmpty() || url.isBlank()) {
				continue;
			}
			LocalDate posted = date(node.path("publishedDate").asString(""));
			SourceJobDetail detail = new SourceJobDetail(description(node.path("description").asString("")), null, null,
					workModel(node.path("workplaceType").asString("")), contractType(node.path("type").asString("")), posted);
			jobs.add(new SourceJob(id, url, title, node.path("careerPageName").asString("").strip(),
					location(node), posted, detail));
		}
		return jobs;
	}

	private static String description(String raw) {
		String text = HtmlText.plain(Jsoup.parse(raw).body());
		return text.isBlank() ? "Sem descrição." : text;
	}

	private static String location(JsonNode node) {
		String place = String.join(", ", Stream.of(node.path("city").asString("").strip(), node.path("state").asString("").strip())
				.filter(part -> !part.isEmpty()).toList());
		return place.isEmpty() && "remote".equals(node.path("workplaceType").asString("")) ? "Remoto" : place;
	}

	private static WorkModel workModel(String value) {
		return switch (value) {
			case "remote" -> WorkModel.REMOTE;
			case "hybrid" -> WorkModel.HYBRID;
			case "on-site" -> WorkModel.ON_SITE;
			default -> null;
		};
	}

	private static ContractType contractType(String value) {
		return switch (value) {
			case "vacancy_type_effective" -> ContractType.CLT;
			case "vacancy_legal_entity" -> ContractType.PJ;
			case "vacancy_type_internship", "vacancy_type_apprentice", "vacancy_type_trainee" -> ContractType.INTERNSHIP;
			case "vacancy_type_temporary", "vacancy_type_freelancer" -> ContractType.TEMPORARY;
			default -> null;
		};
	}

	private static LocalDate date(String value) {
		try {
			return value.isBlank() ? null : OffsetDateTime.parse(value).toLocalDate();
		} catch (DateTimeParseException exception) {
			return null;
		}
	}
}
