package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class GupySource implements JobSource {

	private static final String NAME = "GUPY";
	private static final String SEARCH_PATH = "/job-search/";

	private final RestClient http;
	private final String baseUrl;

	@Autowired
	GupySource(JobSearchProperties properties) {
		this(properties, RestClient.builder().requestFactory(requestFactory(properties)));
	}

	GupySource(JobSearchProperties properties, RestClient.Builder builder) {
		this.baseUrl = properties.gupyBaseUrl();
		this.http = builder
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
		return "Gupy";
	}

	@Override
	public int pageSize() {
		return Integer.MAX_VALUE;
	}

	@Override
	public List<SourceJob> search(SourceQuery query) {
		if (query.start() > 0) {
			return List.of();
		}
		try {
			String html = http.get()
					.uri(URI.create(baseUrl + SEARCH_PATH + GupyQueryParams.of(query)))
					.retrieve()
					.onStatus(HttpStatusCode::isError, (request, response) -> {
						throw new JobSourceException("A Gupy respondeu com erro " + response.getStatusCode().value() + ".");
					})
					.body(String.class);
			return html == null ? List.of() : GupyParser.parse(html);
		} catch (RestClientException exception) {
			throw new JobSourceException("Não deu pra acessar a Gupy. O site pode estar fora do ar ou sem conexão.", exception);
		}
	}

	@Override
	public Optional<SourceJobDetail> detail(String externalId) {
		return Optional.empty();
	}

	private static JdkClientHttpRequestFactory requestFactory(JobSearchProperties properties) {
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(properties.timeout()).build());
		factory.setReadTimeout(properties.timeout());
		return factory;
	}
}
