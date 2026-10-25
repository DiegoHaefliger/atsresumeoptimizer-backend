package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class RemotarSource implements JobSource {

	private static final String NAME = "REMOTAR";
	private static final int PAGE_SIZE = 50;
	private static final Pattern JOB_URL = Pattern.compile("remotar\\.com\\.br/job/(\\d+)");

	private final PublicPageClient client;
	private final String siteUrl;

	@Autowired
	RemotarSource(JobSearchProperties properties) {
		this(properties, PublicPageClient.builderFor(properties));
	}

	RemotarSource(JobSearchProperties properties, RestClient.Builder builder) {
		this.siteUrl = properties.remotarBaseUrl();
		this.client = new PublicPageClient(builder, properties, properties.remotarApiUrl(), "o Remotar");
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String label() {
		return "Remotar";
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
		return client.get("/jobs?search={keyword}&page={page}", query.keyword().strip(), page)
				.map(json -> RemotarParser.parse(json, siteUrl))
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
