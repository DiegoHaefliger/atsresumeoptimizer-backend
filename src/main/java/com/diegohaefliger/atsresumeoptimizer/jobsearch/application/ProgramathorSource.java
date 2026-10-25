package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.net.http.HttpClient;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class ProgramathorSource implements JobSource {

	private static final String NAME = "PROGRAMATHOR";
	private static final int PAGE_SIZE = 30;
	private static final int NOT_FOUND = 404;

	private final RestClient http;
	private final String baseUrl;

	@Autowired
	ProgramathorSource(JobSearchProperties properties) {
		this(properties, RestClient.builder().requestFactory(requestFactory(properties)));
	}

	ProgramathorSource(JobSearchProperties properties, RestClient.Builder builder) {
		this.baseUrl = properties.programathorBaseUrl();
		this.http = builder.baseUrl(baseUrl)
				.defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
				.defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR,pt;q=0.9")
				.build();
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String label() {
		return "Programathor";
	}

	@Override
	public int pageSize() {
		return PAGE_SIZE;
	}

	@Override
	public List<SourceJob> search(SourceQuery query) {
		List<String> slugs = ProgramathorSlugs.of(query.keyword());
		if (slugs.isEmpty()) {
			return List.of();
		}
		int page = query.start() / PAGE_SIZE + 1;
		Map<String, SourceJob> byId = new LinkedHashMap<>();
		for (String slug : slugs) {
			fetch("/jobs-" + slug + "?page=" + page)
					.map(html -> ProgramathorParser.parseList(html, baseUrl))
					.ifPresent(jobs -> jobs.forEach(job -> byId.putIfAbsent(job.externalId(), job)));
		}
		return List.copyOf(byId.values());
	}

	@Override
	public Optional<SourceJobDetail> detail(String externalId) {
		return fetch("/jobs/" + externalId).flatMap(ProgramathorParser::parseDetail);
	}

	private Optional<String> fetch(String path) {
		try {
			return Optional.ofNullable(http.get()
					.uri(path)
					.retrieve()
					.onStatus(status -> status.value() == NOT_FOUND, (request, response) -> {
					})
					.onStatus(HttpStatusCode::isError, (request, response) -> {
						throw new JobSourceException("A Programathor respondeu com erro " + response.getStatusCode().value() + ".");
					})
					.body(String.class));
		} catch (RestClientException exception) {
			throw new JobSourceException("Não deu pra acessar a Programathor. O site pode estar fora do ar ou sem conexão.", exception);
		}
	}

	private static JdkClientHttpRequestFactory requestFactory(JobSearchProperties properties) {
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(properties.timeout()).build());
		factory.setReadTimeout(properties.timeout());
		return factory;
	}
}
