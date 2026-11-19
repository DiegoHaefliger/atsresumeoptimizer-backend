package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class GetOnBoardSource implements JobSource {

	private static final String NAME = "GETONBOARD";
	private static final String BASE_URL = "https://www.getonbrd.com";
	private static final String EXPAND_COMPANY = "[\"company\"]";
	private static final int PAGE_SIZE = 50;
	private static final Pattern JOB_URL = Pattern.compile("getonbrd\\.com/jobs/(?:[a-z]+/)?([a-z0-9][a-z0-9-]+)");

	private final PublicPageClient client;

	@Autowired
	GetOnBoardSource(JobSearchProperties properties) {
		this(properties, PublicPageClient.builderFor(properties), BASE_URL);
	}

	GetOnBoardSource(JobSearchProperties properties, RestClient.Builder builder, String baseUrl) {
		this.client = new PublicPageClient(builder, properties, baseUrl, "o Get on Board");
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String label() {
		return "Get on Board";
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
		return client.get("/api/v0/search/jobs?query={keyword}&per_page={size}&page={page}&expand={expand}",
				query.keyword().strip(), PAGE_SIZE, page, EXPAND_COMPANY)
				.map(GetOnBoardParser::parse)
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
