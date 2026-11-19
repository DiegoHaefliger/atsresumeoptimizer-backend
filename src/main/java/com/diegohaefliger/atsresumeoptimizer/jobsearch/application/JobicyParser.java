package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import com.diegohaefliger.atsresumeoptimizer.job.WorkModel;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.jsoup.Jsoup;
import tools.jackson.databind.JsonNode;

final class JobicyParser {

	private JobicyParser() {
	}

	static List<SourceJob> parse(String json) {
		List<SourceJob> jobs = new ArrayList<>();
		JsonNode data = TolerantJson.parse(json).map(root -> root.path("jobs")).orElse(null);
		if (data == null || !data.isArray()) {
			return jobs;
		}
		for (JsonNode job : data) {
			String id = job.path("id").asString("");
			String title = job.path("jobTitle").asString("").strip();
			String url = job.path("url").asString("").strip();
			if (id.isBlank() || title.isBlank() || url.isBlank()) {
				continue;
			}
			LocalDate postedOn = EpochDates.fromIso(job.path("pubDate").asString(""));
			jobs.add(new SourceJob(id, url, title, job.path("companyName").asString("").strip(),
					job.path("jobGeo").asString("").strip(), postedOn, detail(job, postedOn)));
		}
		return jobs;
	}

	private static SourceJobDetail detail(JsonNode job, LocalDate postedOn) {
		String description = HtmlText.plain(Jsoup.parse(job.path("jobDescription").asString("")).body());
		String employmentType = job.path("jobType").isArray() && !job.path("jobType").isEmpty()
				? job.path("jobType").get(0).asString("") : "";
		return new SourceJobDetail(description.isBlank() ? "Sem descrição." : description,
				job.path("jobLevel").asString(""), employmentType, WorkModel.REMOTE, contractType(employmentType), postedOn);
	}

	private static ContractType contractType(String employmentType) {
		return switch (employmentType.strip().toLowerCase()) {
			case "internship" -> ContractType.INTERNSHIP;
			case "temporary" -> ContractType.TEMPORARY;
			case "contract", "freelance" -> ContractType.PJ;
			default -> null;
		};
	}
}
