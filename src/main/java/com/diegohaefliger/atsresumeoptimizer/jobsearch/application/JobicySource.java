package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class JobicySource implements JobSource {

	private static final String NAME = "JOBICY";
	private static final String BASE_URL = "https://jobicy.com";
	private static final int PAGE_SIZE = 50;
	private static final Pattern JOB_URL = Pattern.compile("jobicy\\.com/jobs/(\\d+)");

	private final PublicPageClient client;

	@Autowired
	JobicySource(JobSearchProperties properties) {
		this(properties, PublicPageClient.builderFor(properties), BASE_URL);
	}

	JobicySource(JobSearchProperties properties, RestClient.Builder builder, String baseUrl) {
		this.client = new PublicPageClient(builder, properties, baseUrl, "o Jobicy");
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String label() {
		return "Jobicy";
	}

	@Override
	public int pageSize() {
		return PAGE_SIZE;
	}

	@Override
	public List<SourceJob> search(SourceQuery query) {
		if (query.start() > 0 || query.keyword() == null || query.keyword().isBlank()) {
			return List.of();
		}
		return client.get("/api/v2/remote-jobs?count={count}&tag={tag}", PAGE_SIZE, query.keyword().strip())
				.map(JobicyParser::parse)
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
