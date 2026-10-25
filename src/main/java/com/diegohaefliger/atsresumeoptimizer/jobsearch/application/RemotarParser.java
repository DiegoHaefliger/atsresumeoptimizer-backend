package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import tools.jackson.databind.JsonNode;

final class RemotarParser {

	private static final String REMOTE_LOCATION = "Remoto";
	private static final int ISO_DATE_LENGTH = 10;

	private RemotarParser() {
	}

	static List<SourceJob> parse(String json, String siteUrl) {
		List<SourceJob> jobs = new ArrayList<>();
		JsonNode data = TolerantJson.parse(json).map(root -> root.path("data")).orElse(null);
		if (data == null || !data.isArray()) {
			return jobs;
		}
		for (JsonNode job : data) {
			String id = job.path("id").asString("");
			String title = job.path("title").asString("").strip();
			if (!id.isBlank() && !title.isBlank() && job.path("active").asBoolean(true) && !job.path("expired").asBoolean(false)) {
				LocalDate postedOn = postedOn(job.path("createdAt").asString(""));
				jobs.add(new SourceJob(id, siteUrl + "/job/" + id, title, job.path("company").path("name").asString(""),
						location(job), postedOn, detail(job, postedOn)));
			}
		}
		return jobs;
	}

	private static SourceJobDetail detail(JsonNode job, LocalDate postedOn) {
		String description = HtmlText.plain(Jsoup.parse(job.path("description").asString("")).body());
		List<String> benefits = new ArrayList<>();
		job.path("jobBenefits").forEach(benefit -> benefits.add(benefit.path("description").asString("").strip()));
		benefits.removeIf(String::isBlank);
		String full = benefits.isEmpty() ? description : description + "\n\nBenefícios: " + String.join(", ", benefits);
		return new SourceJobDetail(full.isBlank() ? "Sem descrição." : full, null, null, workModel(job.path("type").asString("")),
				null, postedOn);
	}

	private static String location(JsonNode job) {
		String city = job.path("city").asString("").strip();
		String state = job.path("state").asString("").strip();
		if (city.isEmpty()) {
			return REMOTE_LOCATION;
		}
		return state.isEmpty() ? city : city + ", " + state;
	}

	private static WorkModel workModel(String type) {
		return switch (type) {
			case "remote" -> WorkModel.REMOTE;
			case "hybrid" -> WorkModel.HYBRID;
			case "onsite", "on_site" -> WorkModel.ON_SITE;
			default -> null;
		};
	}

	private static LocalDate postedOn(String value) {
		try {
			return value.length() < ISO_DATE_LENGTH ? null : LocalDate.parse(value.substring(0, ISO_DATE_LENGTH));
		} catch (DateTimeParseException exception) {
			return null;
		}
	}
}
