package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import com.diegohaefliger.atsresumeoptimizer.NormalizedText;
import com.diegohaefliger.atsresumeoptimizer.job.ContractType;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;

final class InfoJobsParser {

	private static final Pattern JOB_PATH = Pattern.compile("^/vaga-de-([^?#]+)\\.aspx$");
	private static final Pattern CLT = Pattern.compile("\\bclt\\b|efetivo");
	private static final Pattern PJ = Pattern.compile("\\bpj\\b|pessoa juridica|prestador");
	private static final Pattern INTERNSHIP = Pattern.compile("estagio|estagiario");
	private static final Pattern TEMPORARY = Pattern.compile("temporario|temporaria");
	private static final int CONTRACT_HEAD = 400;

	private InfoJobsParser() {
	}

	static List<SourceJob> parseList(String html, String baseUrl) {
		List<SourceJob> jobs = new ArrayList<>();
		for (Element card : Jsoup.parse(html).select("div[id^=vacancy][data-id]")) {
			String href = card.attr("data-href");
			Matcher path = JOB_PATH.matcher(href);
			Element title = card.selectFirst(".js_vacancyTitle");
			if (path.find() && title != null && !title.text().isBlank()) {
				jobs.add(new SourceJob(path.group(1), baseUrl + href, title.text().strip(), company(card), location(card),
						postedOn(card)));
			}
		}
		return jobs;
	}

	static Optional<SourceJobDetail> parseDetail(String html) {
		return JobPostingLd.find(Jsoup.parse(html)).map(posting -> {
			String description = JobPostingLd.description(posting);
			return new SourceJobDetail(description.isBlank() ? "Sem descrição." : description, null, null, null,
					contractType(description), JobPostingLd.postedOn(posting), JobPostingLd.salary(posting));
		});
	}

	private static String company(Element card) {
		Element link = card.selectFirst("a[href*=/empresa-]");
		return link == null ? "" : link.text().strip();
	}

	private static String location(Element card) {
		Element block = card.selectFirst("div.mb-8");
		if (block == null) {
			return "";
		}
		Element copy = block.clone();
		copy.select(".js_divUserVagaDistance").remove();
		return copy.text().strip();
	}

	private static LocalDate postedOn(Element card) {
		Element date = card.selectFirst(".js_date[data-value]");
		String value = date == null ? "" : date.attr("data-value");
		try {
			return value.length() < 10 ? null : LocalDate.parse(value.substring(0, 10).replace('/', '-'));
		} catch (DateTimeParseException exception) {
			return null;
		}
	}

	private static ContractType contractType(String description) {
		String head = NormalizedText.of(description.substring(0, Math.min(CONTRACT_HEAD, description.length())));
		if (INTERNSHIP.matcher(head).find()) {
			return ContractType.INTERNSHIP;
		}
		if (TEMPORARY.matcher(head).find()) {
			return ContractType.TEMPORARY;
		}
		if (PJ.matcher(head).find()) {
			return ContractType.PJ;
		}
		return CLT.matcher(head).find() ? ContractType.CLT : null;
	}
}
