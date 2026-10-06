package com.diegohaefliger.atsresumeoptimizer.googlecalendar.application;

import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleAuthorizationException;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleConnectionStatus;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleCredentials;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleNotConfiguredException;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleTokens;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
class GoogleCalendarServiceImpl implements GoogleCalendarService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GoogleCalendarServiceImpl.class);
	private static final String SETTINGS_PATH = "/settings/notifications";

	private final GoogleAccountStore accountStore;
	private final GoogleCalendarGateway gateway;
	private final GoogleScheduleSync sync;
	private final OAuthStateStore stateStore;
	private final GoogleCalendarProperties properties;

	GoogleCalendarServiceImpl(GoogleAccountStore accountStore, GoogleCalendarGateway gateway, GoogleScheduleSync sync,
			OAuthStateStore stateStore, GoogleCalendarProperties properties) {
		this.accountStore = accountStore;
		this.gateway = gateway;
		this.sync = sync;
		this.stateStore = stateStore;
		this.properties = properties;
	}

	@Override
	public GoogleConnectionStatus status() {
		Optional<GoogleAccountEntity> account = accountStore.account();
		return new GoogleConnectionStatus(
				properties.configured(), account.isPresent(), account.map(GoogleAccountEntity::getAccountEmail).orElse(null),
				properties.redirectUri());
	}

	@Override
	public String authorizationUrl() {
		return gateway.authorizationUrl(credentials(), stateStore.issue());
	}

	@Override
	public String completeAuthorization(String code, String state) {
		try {
			if (!stateStore.consume(state)) {
				throw new GoogleAuthorizationException("Pedido de autorização inválido ou expirado.");
			}
			GoogleTokens tokens = gateway.exchangeCode(credentials(), code);
			accountStore.saveConnection(tokens.refreshToken(), gateway.accountEmail(tokens.accessToken()));
			syncAfterConnecting();
			return redirect("connected");
		} catch (RuntimeException exception) {
			LOGGER.warn("Conexão com o Google não concluída: {}", exception.getMessage());
			return redirect("error");
		}
	}

	@Override
	public int syncAll() {
		return sync.syncAll();
	}

	@Override
	public void disconnect() {
		accountStore.refreshToken().ifPresent(gateway::revoke);
		accountStore.clearConnection();
	}

	private GoogleCredentials credentials() {
		if (!properties.configured()) {
			throw new GoogleNotConfiguredException();
		}
		return new GoogleCredentials(properties.clientId(), properties.clientSecret());
	}

	private void syncAfterConnecting() {
		try {
			sync.syncAll();
		} catch (RuntimeException exception) {
			LOGGER.warn("Conta Google conectada, mas a sincronização inicial falhou: {}", exception.getMessage());
		}
	}

	private String redirect(String result) {
		return UriComponentsBuilder.fromUriString(properties.frontendUrl())
				.path(SETTINGS_PATH)
				.queryParam("google", result)
				.build()
				.toUriString();
	}
}
