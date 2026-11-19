package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import tools.jackson.databind.JsonNode;

final class HimalayasParser {

	private static final String ANYWHERE = "Remoto";

	private HimalayasParser() {
	}

	static List<SourceJob> parse(String json) {
		List<SourceJob> jobs = new ArrayList<>();
		JsonNode data = TolerantJson.parse(json).map(root -> root.path("jobs")).orElse(null);
		if (data == null || !data.isArray()) {
			return jobs;
		}
		for (JsonNode job : data) {
			String id = job.path("guid").asString("").strip();
			String title = job.path("title").asString("").strip();
			String url = job.path("applicationLink").asString("").strip();
			if (id.isBlank() || title.isBlank() || url.isBlank()) {
				continue;
			}
			LocalDate postedOn = EpochDates.fromEpochSeconds(job.path("pubDate").asLong(0));
			jobs.add(new SourceJob(id, url, title, company(job), location(job), postedOn, detail(job, postedOn)));
		}
		return jobs;
	}

	private static String company(JsonNode job) {
		String name = job.path("companyName").asString("").strip();
		return name.isBlank() || name.equalsIgnoreCase("name") ? job.path("companySlug").asString("").strip() : name;
	}

	private static String location(JsonNode job) {
		List<String> places = new ArrayList<>();
		job.path("locationRestrictions").forEach(place -> places.add(place.asString("").strip()));
		places.removeIf(String::isBlank);
		return places.isEmpty() ? ANYWHERE : String.join(", ", places);
	}

	private static SourceJobDetail detail(JsonNode job, LocalDate postedOn) {
		String description = HtmlText.plain(Jsoup.parse(job.path("description").asString("")).body());
		String employmentType = job.path("employmentType").asString("");
		String seniority = job.path("seniority").isArray() && !job.path("seniority").isEmpty()
				? job.path("seniority").get(0).asString("") : "";
		return new SourceJobDetail(description.isBlank() ? "Sem descrição." : description, seniority, employmentType,
				WorkModel.REMOTE, contractType(employmentType), postedOn);
	}

	private static ContractType contractType(String employmentType) {
		return switch (employmentType.strip().toLowerCase()) {
			case "intern", "internship" -> ContractType.INTERNSHIP;
			case "temporary" -> ContractType.TEMPORARY;
			case "contractor", "contract" -> ContractType.PJ;
			default -> null;
		};
	}
}
