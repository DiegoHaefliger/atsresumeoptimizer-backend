package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class VagasSource implements JobSource {

	private static final String NAME = "VAGAS";
	private static final Pattern JOB_URL = Pattern.compile("/vagas/v(\\d+/[^/?#]+)");

	private final PublicPageClient client;
	private final String baseUrl;

	@Autowired
	VagasSource(JobSearchProperties properties) {
		this(properties, PublicPageClient.builderFor(properties));
	}

	VagasSource(JobSearchProperties properties, RestClient.Builder builder) {
		this.baseUrl = properties.vagasBaseUrl();
		this.client = new PublicPageClient(builder, properties, baseUrl, "o Vagas.com");
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String label() {
		return "Vagas.com";
	}

	@Override
	public int pageSize() {
		return Integer.MAX_VALUE;
	}

	@Override
	public List<SourceJob> search(SourceQuery query) {
		String slug = KeywordSlug.of(query.keyword());
		if (slug.isEmpty() || query.start() > 0) {
			return List.of();
		}
		return client.get("/vagas-de-" + slug).map(html -> VagasParser.parseList(html, baseUrl)).orElse(List.of());
	}

	@Override
	public Optional<SourceJobDetail> detail(String externalId) {
		return client.get("/vagas/v" + externalId).flatMap(VagasParser::parseDetail);
	}

	@Override
	public Optional<String> externalIdOf(String url) {
		if (url == null || !url.contains("vagas.com.br")) {
			return Optional.empty();
		}
		Matcher matcher = JOB_URL.matcher(url);
		return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
	}
}
