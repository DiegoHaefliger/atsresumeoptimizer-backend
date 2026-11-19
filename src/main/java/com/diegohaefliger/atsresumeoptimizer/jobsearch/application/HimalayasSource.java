package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class HimalayasSource implements JobSource {

	private static final String NAME = "HIMALAYAS";
	private static final String BASE_URL = "https://himalayas.app";
	private static final int PAGE_SIZE = 20;
	private static final Pattern JOB_URL = Pattern.compile("(https?://himalayas\\.app/companies/[^/?#]+/jobs/[^/?#]+)");

	private final PublicPageClient client;

	@Autowired
	HimalayasSource(JobSearchProperties properties) {
		this(properties, PublicPageClient.builderFor(properties), BASE_URL);
	}

	HimalayasSource(JobSearchProperties properties, RestClient.Builder builder, String baseUrl) {
		this.client = new PublicPageClient(builder, properties, baseUrl, "o Himalayas");
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String label() {
		return "Himalayas";
	}

	@Override
	public int pageSize() {
		return PAGE_SIZE;
	}

	@Override
	public List<SourceJob> search(SourceQuery query) {
		if (query.keyword() == null || query.keyword().isBlank()) {
			return List.of();
		}
		int page = query.start() / PAGE_SIZE + 1;
		return client.get("/jobs/api/search?q={keyword}&page={page}", query.keyword().strip(), page)
				.map(HimalayasParser::parse)
				.orElse(List.of());
	}

	@Override
	public Optional<SourceJobDetail> detail(String externalId) {
		return Optional.empty();
	}

	@Override
	public Optional<String> externalIdOf(String url) {
		Matcher matcher = url == null ? null : JOB_URL.matcher(url);
		return matcher != null && matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
	}
}
