package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class LinkedInGuestSource implements JobSource {

	private static final String NAME = "LINKEDIN";
	private static final int PAGE_SIZE = 10;
	private static final String SEARCH_PATH = "/jobs-guest/jobs/api/seeMoreJobPostings/search";
	private static final String DETAIL_PATH = "/jobs-guest/jobs/api/jobPosting/{id}";
	private static final int TOO_MANY_REQUESTS = 429;
	private static final Pattern JOB_ID = Pattern.compile("(\\d{7,})(?:[/?#]|$)");

	private final RestClient http;

	@Autowired
	LinkedInGuestSource(JobSearchProperties properties) {
		this(properties, RestClient.builder().requestFactory(requestFactory(properties)));
	}

	LinkedInGuestSource(JobSearchProperties properties, RestClient.Builder builder) {
		this.http = builder.baseUrl(properties.baseUrl())
				.defaultHeader(HttpHeaders.USER_AGENT, properties.userAgent())
				.defaultHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9")
				.build();
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public String label() {
		return "LinkedIn";
	}

	@Override
	public Optional<String> externalIdOf(String url) {
		Matcher matcher = JOB_ID.matcher(url == null ? "" : url);
		return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
	}

	@Override
	public int pageSize() {
		return PAGE_SIZE;
	}

	@Override
	public List<SourceJob> search(SourceQuery query) {
		Map<String, String> params = LinkedInQueryParams.of(query);
		String html = fetch(() -> http.get()
				.uri(builder -> {
					builder.path(SEARCH_PATH);
					params.keySet().forEach(name -> builder.queryParam(name, "{" + name + "}"));
					return builder.build(params);
				})
				.retrieve()
				.onStatus(HttpStatusCode::isError, (request, response) -> {
					throw failure(response.getStatusCode().value());
				})
				.body(String.class));
		return html == null ? List.of() : LinkedInHtmlParser.parseSearch(html);
	}

	@Override
	public Optional<SourceJobDetail> detail(String externalId) {
		String html = fetch(() -> http.get()
				.uri(DETAIL_PATH, externalId)
				.retrieve()
				.onStatus(status -> status.value() == 404, (request, response) -> {
				})
				.onStatus(HttpStatusCode::isError, (request, response) -> {
					throw failure(response.getStatusCode().value());
				})
				.body(String.class));
		return html == null ? Optional.empty() : LinkedInHtmlParser.parseDetail(html);
	}

	private String fetch(java.util.function.Supplier<String> request) {
		try {
			return request.get();
		} catch (RestClientException exception) {
			throw new JobSourceException("Não deu pra acessar o LinkedIn. O site pode estar fora do ar ou sem conexão.", exception);
		}
	}

	private static JobSourceException failure(int status) {
		return status == TOO_MANY_REQUESTS
				? new JobSourceException("O LinkedIn limitou as requisições. Tente de novo mais tarde.")
				: new JobSourceException("O LinkedIn respondeu com erro " + status + ".");
	}

	private static JdkClientHttpRequestFactory requestFactory(JobSearchProperties properties) {
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(properties.timeout()).build());
		factory.setReadTimeout(properties.timeout());
		return factory;
	}
}
