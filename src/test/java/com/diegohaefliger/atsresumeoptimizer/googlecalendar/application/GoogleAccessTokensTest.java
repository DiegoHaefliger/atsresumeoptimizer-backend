package com.diegohaefliger.atsresumeoptimizer.googlecalendar.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleAuthRevokedException;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleCredentials;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoogleAccessTokensTest {

	@Mock
	private GoogleAccountStore accountStore;

	@Mock
	private GoogleCalendarGateway gateway;

	private GoogleAccessTokens tokens(String clientId) {
		return new GoogleAccessTokens(accountStore,
				new GoogleCalendarProperties(clientId, "secret", "uri", "front", "a", "t", "r", "u", "e"), gateway);
	}

	@Test
	void refreshesTheAccessTokenOfTheConnectedAccount() {
		when(accountStore.refreshToken()).thenReturn(Optional.of("ref"));
		when(gateway.refreshAccessToken(new GoogleCredentials("id", "secret"), "ref")).thenReturn("acc");

		assertThat(tokens("id").current()).contains("acc");
	}

	@Test
	void isEmptyWithoutAccountOrServerCredentials() {
		when(accountStore.refreshToken()).thenReturn(Optional.empty()).thenReturn(Optional.of("ref"));

		assertThat(tokens("id").current()).isEmpty();
		assertThat(tokens("").current()).isEmpty();
		verifyNoInteractions(gateway);
	}

	@Test
	void disconnectsWhenGoogleRevokedTheAccess() {
		when(accountStore.refreshToken()).thenReturn(Optional.of("ref"));
		when(gateway.refreshAccessToken(any(), any())).thenThrow(new GoogleAuthRevokedException());

		assertThat(tokens("id").current()).isEmpty();
		verify(accountStore).clearConnection();
	}
}
