package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class InfoJobsSource implements JobSource {

	private static final String NAME = "INFOJOBS";
	private static final int PAGE_SIZE = 20;
	private static final Pattern JOB_URL = Pattern.compile("/vaga-de-([^?#]+)\\.aspx");

	private final PublicPageClient client;
	private final String baseUrl;

	@Autowired
	InfoJobsSource(JobSearchProperties properties) {
		this(properties, PublicPageClient.builderFor(properties));
	}

	InfoJobsSource(JobSearchProperties properties, RestClient.Builder builder) {
		this.baseUrl = properties.infojobsBaseUrl();
		this.client = new PublicPageClient(builder, properties, baseUrl, "o InfoJobs");
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String label() {
		return "InfoJobs";
	}

	@Override
	public int pageSize() {
		return PAGE_SIZE;
	}

	@Override
	public List<SourceJob> search(SourceQuery query) {
		String slug = KeywordSlug.of(query.keyword());
		if (slug.isEmpty()) {
			return List.of();
		}
		int page = query.start() / PAGE_SIZE + 1;
		String path = "/vagas-de-emprego-" + slug + ".aspx" + (page > 1 ? "?Page=" + page : "");
		return client.get(path).map(html -> InfoJobsParser.parseList(html, baseUrl)).orElse(List.of());
	}

	@Override
	public Optional<SourceJobDetail> detail(String externalId) {
		return client.get("/vaga-de-" + externalId + ".aspx").flatMap(InfoJobsParser::parseDetail);
	}

	@Override
	public Optional<String> externalIdOf(String url) {
		if (url == null || !url.contains("infojobs.com.br")) {
			return Optional.empty();
		}
		Matcher matcher = JOB_URL.matcher(url);
		return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
	}
}
