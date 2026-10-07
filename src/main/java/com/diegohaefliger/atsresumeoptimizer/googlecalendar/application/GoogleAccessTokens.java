package com.diegohaefliger.atsresumeoptimizer.googlecalendar.application;

import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleAuthRevokedException;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleCredentials;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class GoogleAccessTokens {

	private static final Logger LOGGER = LoggerFactory.getLogger(GoogleAccessTokens.class);

	private final GoogleAccountStore accountStore;
	private final GoogleCalendarProperties properties;
	private final GoogleCalendarGateway gateway;

	GoogleAccessTokens(GoogleAccountStore accountStore, GoogleCalendarProperties properties, GoogleCalendarGateway gateway) {
		this.accountStore = accountStore;
		this.properties = properties;
		this.gateway = gateway;
	}

	Optional<String> current() {
		Optional<String> refreshToken = accountStore.refreshToken();
		if (!properties.configured() || refreshToken.isEmpty()) {
			return Optional.empty();
		}
		try {
			GoogleCredentials credentials = new GoogleCredentials(properties.clientId(), properties.clientSecret());
			return Optional.of(gateway.refreshAccessToken(credentials, refreshToken.get()));
		} catch (GoogleAuthRevokedException exception) {
			LOGGER.warn("Acesso ao Google revogado; conta desconectada.");
			accountStore.clearConnection();
			return Optional.empty();
		}
	}
}
