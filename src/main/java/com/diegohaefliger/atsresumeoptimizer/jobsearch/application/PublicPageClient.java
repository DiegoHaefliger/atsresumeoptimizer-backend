package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.net.http.HttpClient;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

final class PublicPageClient {

	private static final int NOT_FOUND = 404;

	private final RestClient http;
	private final String label;

	PublicPageClient(RestClient.Builder builder, JobSearchProperties properties, String baseUrl, String label) {
		this.label = label;
		this.http = builder.baseUrl(baseUrl)
				.defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
				.defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR,pt;q=0.9")
				.build();
	}

	static RestClient.Builder builderFor(JobSearchProperties properties) {
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(properties.timeout()).build());
		factory.setReadTimeout(properties.timeout());
		return RestClient.builder().requestFactory(factory);
	}

	Optional<String> get(String path, Object... variables) {
		try {
			return Optional.ofNullable(http.get()
					.uri(path, variables)
					.retrieve()
					.onStatus(status -> status.value() == NOT_FOUND, (request, response) -> {
					})
					.onStatus(HttpStatusCode::isError, (request, response) -> {
						throw new JobSourceException(label + " respondeu com erro " + response.getStatusCode().value() + ".");
					})
					.body(String.class));
		} catch (RestClientException exception) {
			throw new JobSourceException("Não deu pra acessar " + label + ". O site pode estar fora do ar ou sem conexão.", exception);
		}
	}
}
