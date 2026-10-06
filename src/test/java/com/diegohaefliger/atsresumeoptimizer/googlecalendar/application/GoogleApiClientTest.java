package com.diegohaefliger.atsresumeoptimizer.googlecalendar.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleApiException;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleAuthRevokedException;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleAuthorizationException;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleCredentials;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleTokens;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class GoogleApiClientTest {

	private static final String EVENTS = "https://api.test/events";
	private static final GoogleCredentials CREDENTIALS = new GoogleCredentials("id-123", "segredo");

	private final RestClient.Builder builder = RestClient.builder();
	private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
	private final GoogleApiClient client = new GoogleApiClient(new GoogleCalendarProperties(
			"id-123", "segredo", "http://localhost:8080/cb", "http://localhost:5173", "https://auth.test/authorize", "https://auth.test/token",
			"https://auth.test/revoke", "https://auth.test/userinfo", EVENTS), builder);

	@Test
	void buildsTheAuthorizationUrlWithOfflineAccessAndState() {
		String url = client.authorizationUrl(CREDENTIALS, "abc");

		assertThat(url).startsWith("https://auth.test/authorize?")
				.contains("client_id=id-123", "response_type=code", "access_type=offline", "prompt=consent", "state=abc")
				.contains("redirect_uri=http://localhost:8080/cb")
				.contains("calendar.events");
	}

	@Test
	void exchangesTheCodeForTokens() {
		server.expect(requestTo("https://auth.test/token"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().string(containsString("grant_type=authorization_code")))
				.andExpect(content().string(containsString("code=c0de")))
				.andRespond(withSuccess("{\"access_token\":\"acc\",\"refresh_token\":\"ref\"}", MediaType.APPLICATION_JSON));

		assertThat(client.exchangeCode(CREDENTIALS, "c0de")).isEqualTo(new GoogleTokens("acc", "ref"));
	}

	@Test
	void refusesAnExchangeWithoutRefreshTokenOrRejectedByGoogle() {
		server.expect(requestTo("https://auth.test/token"))
				.andRespond(withSuccess("{\"access_token\":\"acc\"}", MediaType.APPLICATION_JSON));
		assertThatThrownBy(() -> client.exchangeCode(CREDENTIALS, "x")).isInstanceOf(GoogleAuthorizationException.class);

		server.reset();
		server.expect(requestTo("https://auth.test/token")).andRespond(withStatus(HttpStatus.BAD_REQUEST));
		assertThatThrownBy(() -> client.exchangeCode(CREDENTIALS, "x")).isInstanceOf(GoogleAuthorizationException.class);
	}

	@Test
	void refreshesTheAccessTokenAndFlagsARevokedGrant() {
		server.expect(requestTo("https://auth.test/token"))
				.andExpect(content().string(containsString("grant_type=refresh_token")))
				.andRespond(withSuccess("{\"access_token\":\"novo\"}", MediaType.APPLICATION_JSON));
		assertThat(client.refreshAccessToken(CREDENTIALS, "ref")).isEqualTo("novo");

		server.reset();
		server.expect(requestTo("https://auth.test/token")).andRespond(withStatus(HttpStatus.BAD_REQUEST));
		assertThatThrownBy(() -> client.refreshAccessToken(CREDENTIALS, "ref")).isInstanceOf(GoogleAuthRevokedException.class);

		server.reset();
		server.expect(requestTo("https://auth.test/token")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
		assertThatThrownBy(() -> client.refreshAccessToken(CREDENTIALS, "ref")).isInstanceOf(GoogleApiException.class);
	}

	@Test
	void readsTheAccountEmail() {
		server.expect(requestTo("https://auth.test/userinfo"))
				.andExpect(header("Authorization", "Bearer acc"))
				.andRespond(withSuccess("{\"email\":\"eu@example.com\"}", MediaType.APPLICATION_JSON));

		assertThat(client.accountEmail("acc")).isEqualTo("eu@example.com");
	}

	@Test
	void createsAnEventWhenThereIsNoLinkYet() {
		server.expect(requestTo(EVENTS))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer acc"))
				.andRespond(withSuccess("{\"id\":\"evt1\"}", MediaType.APPLICATION_JSON));

		assertThat(client.saveEvent("acc", null, Map.of("summary", "x"))).isEqualTo("evt1");
	}

	@Test
	void updatesAnExistingEventAndRecreatesItWhenGoogleNoLongerHasIt() {
		server.expect(requestTo(EVENTS + "/evt1"))
				.andExpect(method(HttpMethod.PUT))
				.andRespond(withSuccess("{\"id\":\"evt1\"}", MediaType.APPLICATION_JSON));
		assertThat(client.saveEvent("acc", "evt1", Map.of("summary", "x"))).isEqualTo("evt1");

		server.reset();
		server.expect(requestTo(EVENTS + "/evt1")).andExpect(method(HttpMethod.PUT)).andRespond(withResourceNotFound());
		server.expect(requestTo(EVENTS)).andExpect(method(HttpMethod.POST))
				.andRespond(withSuccess("{\"id\":\"evt2\"}", MediaType.APPLICATION_JSON));
		assertThat(client.saveEvent("acc", "evt1", Map.of("summary", "x"))).isEqualTo("evt2");
	}

	@Test
	void deletesEventsAndIgnoresTheOnesAlreadyGone() {
		server.expect(requestTo(EVENTS + "/evt1")).andExpect(method(HttpMethod.DELETE)).andRespond(withNoContent());
		client.deleteEvent("acc", "evt1");

		server.reset();
		server.expect(requestTo(EVENTS + "/evt2")).andRespond(withStatus(HttpStatus.GONE));
		client.deleteEvent("acc", "evt2");

		server.reset();
		server.expect(requestTo(EVENTS + "/evt3")).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
		assertThatThrownBy(() -> client.deleteEvent("acc", "evt3")).isInstanceOf(GoogleApiException.class);
	}

	@Test
	void revokeNeverThrows() {
		server.expect(requestTo("https://auth.test/revoke")).andRespond(withStatus(HttpStatus.BAD_REQUEST));

		client.revoke("ref");

		server.verify();
	}
}
