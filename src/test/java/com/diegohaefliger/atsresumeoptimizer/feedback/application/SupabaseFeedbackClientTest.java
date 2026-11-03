package com.diegohaefliger.atsresumeoptimizer.feedback.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackAttachment;
import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackSubmission;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class SupabaseFeedbackClientTest {

	private static final String URL = "https://projeto.supabase.co";
	private static final String ROWS = URL + "/rest/v1/feedback";
	private static final UUID ID = UUID.fromString("0198a000-0000-7000-8000-000000000001");
	private static final String IMAGE_URL = URL + "/storage/v1/object/feedback-screenshots/" + ID + "/0.png";
	private static final FeedbackSubmission SUBMISSION = new FeedbackSubmission(5,
			"Consegui a entrevista", "ana@exemplo.com", true, "1.4.0");
	private static final FeedbackAttachment IMAGE = new FeedbackAttachment("tela.png", "image/png",
			new byte[] {(byte) 0x89, 'P', 'N', 'G', 1});

	private final RestClient.Builder builder = RestClient.builder();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

	private SupabaseFeedbackClient client(String url, String key) {
		return new SupabaseFeedbackClient(
				new FeedbackProperties(url, key, "feedback", "feedback-screenshots", Duration.ofSeconds(5), 20), builder);
	}

	@Test
	void insertsTheRowWithTheLocalIdAndPublishableKey() {
		server.expect(requestTo(ROWS))
				.andExpect(header("apikey", "sb_publishable_x"))
				.andExpect(header("Authorization", "Bearer sb_publishable_x"))
				.andExpect(header("Prefer", "return=minimal"))
				.andExpect(jsonPath("$.id").value(ID.toString()))
				.andExpect(jsonPath("$.rating").value(5))
				.andExpect(jsonPath("$.contact").value("ana@exemplo.com"))
				.andExpect(jsonPath("$.allow_publish").value(true))
				.andExpect(jsonPath("$.app_version").value("1.4.0"))
				.andExpect(jsonPath("$.screenshots").isEmpty())
				.andRespond(withStatus(HttpStatus.CREATED));

		assertThat(client(URL, "sb_publishable_x").deliver(ID, SUBMISSION, List.of())).isTrue();
		server.verify();
	}

	@Test
	void uploadsEachImageBeforeInsertingTheRowThatPointsToThem() {
		server.expect(requestTo(IMAGE_URL))
				.andExpect(content().contentType(MediaType.IMAGE_PNG))
				.andExpect(content().bytes(IMAGE.content()))
				.andRespond(withStatus(HttpStatus.OK));
		server.expect(requestTo(ROWS))
				.andExpect(jsonPath("$.screenshots[0]").value(ID + "/0.png"))
				.andRespond(withStatus(HttpStatus.CREATED));

		assertThat(client(URL, "chave").deliver(ID, SUBMISSION, List.of(IMAGE))).isTrue();
		server.verify();
	}

	@Test
	void treatsAnAlreadyStoredImageAndRowAsDelivered() {
		server.expect(requestTo(IMAGE_URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
				.contentType(MediaType.APPLICATION_JSON).body("{\"error\":\"Duplicate\",\"statusCode\":\"409\"}"));
		server.expect(requestTo(ROWS)).andRespond(withStatus(HttpStatus.CONFLICT));

		assertThat(client(URL, "chave").deliver(ID, SUBMISSION, List.of(IMAGE))).isTrue();
	}

	@Test
	void reportsFailureWhenTheServerRefusesOrBreaks() {
		server.expect(requestTo(ROWS)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
		assertThat(client(URL, "chave").deliver(ID, SUBMISSION, List.of())).isFalse();

		server.reset();
		server.expect(requestTo(ROWS)).andRespond(withServerError());
		assertThat(client(URL, "chave").deliver(ID, SUBMISSION, List.of())).isFalse();
	}

	@Test
	void doesNotInsertTheRowWhenAnUploadFails() {
		server.expect(requestTo(IMAGE_URL)).andRespond(withServerError());

		assertThat(client(URL, "chave").deliver(ID, SUBMISSION, List.of(IMAGE))).isFalse();
		server.verify();
	}

	@Test
	void sendsNothingWithoutUrlAndKey() {
		assertThat(client("", "").deliver(ID, SUBMISSION, List.of())).isFalse();
		server.verify();
	}
}
