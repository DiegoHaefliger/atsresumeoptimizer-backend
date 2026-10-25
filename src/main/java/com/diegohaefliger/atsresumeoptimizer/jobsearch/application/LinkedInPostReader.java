package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class LinkedInPostReader {

	private final RestClient http;

	@Autowired
	LinkedInPostReader(JobSearchProperties properties) {
		this(properties, RestClient.builder().requestFactory(requestFactory(properties)));
	}

	LinkedInPostReader(JobSearchProperties properties, RestClient.Builder builder) {
		this.http = builder
				.defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
				.defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "pt-BR,pt;q=0.9,en;q=0.8")
				.build();
	}

	Optional<String> read(URI link) {
		try {
			String html = http.get()
					.uri(link)
					.retrieve()
					.onStatus(HttpStatusCode::isError, (request, response) -> {
					})
					.body(String.class);
			return LinkedInPostParser.parse(html);
		} catch (RestClientException exception) {
			return Optional.empty();
		}
	}

	private static JdkClientHttpRequestFactory requestFactory(JobSearchProperties properties) {
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(properties.timeout()).build());
		factory.setReadTimeout(properties.timeout());
		return factory;
	}
}
