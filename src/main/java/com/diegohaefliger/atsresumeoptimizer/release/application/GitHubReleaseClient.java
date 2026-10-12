package com.diegohaefliger.atsresumeoptimizer.release.application;

import com.diegohaefliger.atsresumeoptimizer.release.domain.AppVersion;
import com.diegohaefliger.atsresumeoptimizer.release.domain.LatestRelease;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.http.HttpClient;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class GitHubReleaseClient implements ReleaseGateway {

	private static final Logger LOGGER = LoggerFactory.getLogger(GitHubReleaseClient.class);
	private static final String GITHUB_JSON = "application/vnd.github+json";
	private static final String USER_AGENT = "ATSResumeOptimizer";

	private final ReleaseProperties properties;
	private final RestClient http;

	@Autowired
	GitHubReleaseClient(ReleaseProperties properties) {
		this(properties, RestClient.builder().requestFactory(requestFactory(properties)));
	}

	GitHubReleaseClient(ReleaseProperties properties, RestClient.Builder builder) {
		this.properties = properties;
		this.http = builder.baseUrl(properties.apiUrl())
				.defaultHeader(HttpHeaders.ACCEPT, GITHUB_JSON)
				.defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
				.build();
	}

	@Override
	public Optional<LatestRelease> latest() {
		try {
			GitHubRelease release = http.get()
					.uri("/repos/{owner}/{name}/releases/latest", properties.owner(), properties.name())
					.retrieve()
					.body(GitHubRelease.class);
			if (release == null || release.draft() || release.prerelease()) {
				return Optional.empty();
			}
			return AppVersion.parse(release.tagName())
					.map(version -> new LatestRelease(version, release.htmlUrl(), release.publishedAt()));
		} catch (RestClientException exception) {
			LOGGER.info("Não deu pra consultar a última versão no GitHub: {}", exception.getMessage());
			return Optional.empty();
		}
	}

	private static JdkClientHttpRequestFactory requestFactory(ReleaseProperties properties) {
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(properties.timeout()).build());
		factory.setReadTimeout(properties.timeout());
		return factory;
	}

	record GitHubRelease(
			@JsonProperty("tag_name") String tagName,
			@JsonProperty("html_url") String htmlUrl,
			@JsonProperty("published_at") Instant publishedAt,
			boolean draft,
			boolean prerelease) {
	}
}
