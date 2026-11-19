package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import tools.jackson.databind.JsonNode;

final class GetOnBoardParser {

	private static final String REMOTE_LOCATION = "Remoto";

	private GetOnBoardParser() {
	}

	static List<SourceJob> parse(String json) {
		List<SourceJob> jobs = new ArrayList<>();
		JsonNode data = TolerantJson.parse(json).map(root -> root.path("data")).orElse(null);
		if (data == null || !data.isArray()) {
			return jobs;
		}
		for (JsonNode job : data) {
			JsonNode attributes = job.path("attributes");
			String id = job.path("id").asString("").strip();
			String title = attributes.path("title").asString("").strip();
			String url = job.path("links").path("public_url").asString("").strip();
			if (id.isBlank() || title.isBlank() || url.isBlank()) {
				continue;
			}
			LocalDate postedOn = EpochDates.fromEpochSeconds(attributes.path("published_at").asLong(0));
			jobs.add(new SourceJob(id, url, title, attributes.path("company").path("data").path("attributes").path("name")
					.asString("").strip(), location(attributes), postedOn, detail(attributes, postedOn)));
		}
		return jobs;
	}

	private static String location(JsonNode attributes) {
		List<String> countries = new ArrayList<>();
		attributes.path("countries").forEach(country -> countries.add(country.asString("").strip()));
		countries.removeIf(String::isBlank);
		return countries.isEmpty() ? REMOTE_LOCATION : String.join(", ", countries);
	}

	private static SourceJobDetail detail(JsonNode attributes, LocalDate postedOn) {
		StringBuilder markup = new StringBuilder();
		for (String section : new String[] {"functions", "description", "desirable", "benefits"}) {
			markup.append(attributes.path(section).asString(""));
		}
		String description = HtmlText.plain(Jsoup.parse(markup.toString()).body());
		return new SourceJobDetail(description.isBlank() ? "Sem descrição." : description, null, null,
				workModel(attributes.path("remote_modality").asString("")), null, postedOn);
	}

	private static WorkModel workModel(String modality) {
		return switch (modality) {
			case "fully_remote", "remote" -> WorkModel.REMOTE;
			case "hybrid" -> WorkModel.HYBRID;
			case "no_remote", "on_site", "onsite" -> WorkModel.ON_SITE;
			default -> null;
		};
	}
}
