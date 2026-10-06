package com.diegohaefliger.atsresumeoptimizer.googlecalendar.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleApiException;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleCredentials;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleNotConfiguredException;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleTokens;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoogleCalendarServiceImplTest {

	private static final GoogleCredentials CREDENTIALS = new GoogleCredentials("id", "secret");

	@Mock
	private GoogleAccountStore accountStore;

	@Mock
	private GoogleCalendarGateway gateway;

	@Mock
	private GoogleScheduleSync sync;

	private final OAuthStateStore stateStore =
			new OAuthStateStore(Clock.fixed(Instant.parse("2026-10-06T12:00:00Z"), ZoneOffset.UTC));

	private GoogleCalendarServiceImpl service(String clientId, String clientSecret) {
		return new GoogleCalendarServiceImpl(accountStore, gateway, sync, stateStore, new GoogleCalendarProperties(
				clientId, clientSecret, "http://localhost:8080/cb", "http://localhost:5173", "a", "t", "r", "u", "e"));
	}

	private GoogleCalendarServiceImpl service() {
		return service("id", "secret");
	}

	private GoogleAccountEntity account() {
		GoogleAccountEntity account = new GoogleAccountEntity(UUID.randomUUID());
		account.setRefreshToken("x");
		account.setAccountEmail("eu@example.com");
		return account;
	}

	@Test
	void reportsWhetherTheServerIsConfiguredAndTheAccountConnected() {
		when(accountStore.account()).thenReturn(Optional.empty()).thenReturn(Optional.of(account()));

		var disconnected = service().status();
		assertThat(disconnected.configured()).isTrue();
		assertThat(disconnected.connected()).isFalse();

		var connected = service().status();
		assertThat(connected.connected()).isTrue();
		assertThat(connected.accountEmail()).isEqualTo("eu@example.com");
		assertThat(connected.redirectUri()).isEqualTo("http://localhost:8080/cb");
		assertThat(service("", "").status().configured()).isFalse();
	}

	@Test
	void refusesToAuthorizeWhenTheServerHasNoCredentials() {
		assertThatThrownBy(() -> service("", "").authorizationUrl()).isInstanceOf(GoogleNotConfiguredException.class);
	}

	@Test
	void completesTheAuthorizationAndSyncsEverything() {
		when(gateway.authorizationUrl(any(), any())).thenReturn("https://google/auth");
		GoogleCalendarServiceImpl service = service();
		service.authorizationUrl();
		when(gateway.exchangeCode(CREDENTIALS, "c0de")).thenReturn(new GoogleTokens("acc", "ref"));
		when(gateway.accountEmail("acc")).thenReturn("eu@example.com");

		String redirect = service.completeAuthorization("c0de", captureState());

		assertThat(redirect).isEqualTo("http://localhost:5173/settings/notifications?google=connected");
		verify(accountStore).saveConnection("ref", "eu@example.com");
		verify(sync).syncAll();
	}

	@Test
	void stillReportsConnectedWhenOnlyTheFirstSyncFails() {
		when(gateway.authorizationUrl(any(), any())).thenReturn("https://google/auth");
		GoogleCalendarServiceImpl service = service();
		service.authorizationUrl();
		when(gateway.exchangeCode(any(), any())).thenReturn(new GoogleTokens("acc", "ref"));
		when(gateway.accountEmail("acc")).thenReturn("eu@example.com");
		doThrow(new GoogleApiException("falha", null)).when(sync).syncAll();

		assertThat(service.completeAuthorization("c0de", captureState())).endsWith("google=connected");
	}

	@Test
	void redirectsWithAnErrorForAnUnknownStateOrMissingCode() {
		assertThat(service().completeAuthorization("c0de", "forjado")).endsWith("google=error");
		assertThat(service().completeAuthorization(null, null)).endsWith("google=error");
		verify(accountStore, never()).saveConnection(any(), any());
	}

	@Test
	void disconnectRevokesTheTokenAndClearsTheConnection() {
		when(accountStore.refreshToken()).thenReturn(Optional.of("ref"));

		service().disconnect();

		verify(gateway).revoke("ref");
		verify(accountStore).clearConnection();
	}

	private String captureState() {
		ArgumentCaptor<String> state = ArgumentCaptor.forClass(String.class);
		verify(gateway).authorizationUrl(any(), state.capture());
		return state.getValue();
	}
}
