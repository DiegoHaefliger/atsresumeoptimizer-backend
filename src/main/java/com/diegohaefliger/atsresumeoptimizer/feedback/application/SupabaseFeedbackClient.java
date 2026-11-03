package com.diegohaefliger.atsresumeoptimizer.feedback.application;

import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackAttachment;
import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackSubmission;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.net.http.HttpClient;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class SupabaseFeedbackClient implements FeedbackGateway {

	private static final Logger LOGGER = LoggerFactory.getLogger(SupabaseFeedbackClient.class);
	private static final String API_KEY_HEADER = "apikey";
	private static final String PREFER_HEADER = "Prefer";
	private static final String RETURN_MINIMAL = "return=minimal";
	private static final String DUPLICATE_MARKER = "Duplicate";

	private final FeedbackProperties properties;
	private final RestClient http;

	@Autowired
	SupabaseFeedbackClient(FeedbackProperties properties) {
		this(properties, RestClient.builder().requestFactory(requestFactory(properties)));
	}

	SupabaseFeedbackClient(FeedbackProperties properties, RestClient.Builder builder) {
		this.properties = properties;
		this.http = builder.baseUrl(properties.url())
				.defaultHeader(API_KEY_HEADER, properties.publishableKey())
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.publishableKey())
				.build();
	}

	@Override
	public boolean deliver(UUID id, FeedbackSubmission submission, List<FeedbackAttachment> attachments) {
		if (!properties.enabled()) {
			return false;
		}
		try {
			List<String> paths = IntStream.range(0, attachments.size())
					.mapToObj(position -> upload(id, position, attachments.get(position)))
					.toList();
			insert(row(id, submission, paths));
			return true;
		} catch (HttpClientErrorException.Conflict alreadyThere) {
			return true;
		} catch (RestClientException exception) {
			LOGGER.info("Não deu pra enviar a avaliação: {}", exception.getMessage());
			return false;
		}
	}

	private String upload(UUID id, int position, FeedbackAttachment attachment) {
		String fileName = "%d.%s".formatted(position, FeedbackAttachmentRules.extension(attachment.contentType()));
		try {
			http.post()
					.uri("/storage/v1/object/{bucket}/{id}/{file}", properties.bucket(), id, fileName)
					.contentType(MediaType.parseMediaType(attachment.contentType()))
					.body(attachment.content())
					.retrieve()
					.toBodilessEntity();
		} catch (HttpClientErrorException exception) {
			if (!exception.getResponseBodyAsString().contains(DUPLICATE_MARKER)) {
				throw exception;
			}
		}
		return "%s/%s".formatted(id, fileName);
	}

	private void insert(FeedbackRow row) {
		http.post()
				.uri("/rest/v1/{table}", properties.table())
				.header(PREFER_HEADER, RETURN_MINIMAL)
				.contentType(MediaType.APPLICATION_JSON)
				.body(row)
				.retrieve()
				.toBodilessEntity();
	}

	private static FeedbackRow row(UUID id, FeedbackSubmission submission, List<String> screenshots) {
		return new FeedbackRow(
				id.toString(),
				submission.rating(),
				StringUtils.hasText(submission.message()) ? submission.message() : null,
				StringUtils.hasText(submission.contact()) ? submission.contact() : null,
				submission.allowPublish(),
				submission.appVersion(),
				screenshots);
	}

	private static JdkClientHttpRequestFactory requestFactory(FeedbackProperties properties) {
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
				HttpClient.newBuilder().connectTimeout(properties.timeout()).build());
		factory.setReadTimeout(properties.timeout());
		return factory;
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	record FeedbackRow(
			String id,
			int rating,
			String message,
			String contact,
			@JsonProperty("allow_publish") boolean allowPublish,
			@JsonProperty("app_version") String appVersion,
			List<String> screenshots) {
	}
}
