package com.diegohaefliger.atsresumeoptimizer.googlecalendar.application;

import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleAuthorizationException;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleCalendarItem;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleConnectionStatus;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.InvalidCalendarRangeException;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleCredentials;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleNotConfiguredException;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleTokens;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

@Service
class GoogleCalendarServiceImpl implements GoogleCalendarService {

	private static final Logger LOGGER = LoggerFactory.getLogger(GoogleCalendarServiceImpl.class);
	private static final String SETTINGS_PATH = "/settings/notifications";
	private static final Duration MAX_RANGE = Duration.ofDays(366);

	private final GoogleAccountStore accountStore;
	private final GoogleCalendarGateway gateway;
	private final GoogleScheduleSync sync;
	private final GoogleAccessTokens accessTokens;
	private final GoogleEventLinkRepository linkRepository;
	private final OAuthStateStore stateStore;
	private final GoogleCalendarProperties properties;

	GoogleCalendarServiceImpl(GoogleAccountStore accountStore, GoogleCalendarGateway gateway, GoogleScheduleSync sync,
			GoogleAccessTokens accessTokens, GoogleEventLinkRepository linkRepository, OAuthStateStore stateStore,
			GoogleCalendarProperties properties) {
		this.accountStore = accountStore;
		this.gateway = gateway;
		this.sync = sync;
		this.accessTokens = accessTokens;
		this.linkRepository = linkRepository;
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
	@Transactional(readOnly = true)
	public List<GoogleCalendarItem> events(Instant from, Instant to) {
		if (!from.isBefore(to) || Duration.between(from, to).compareTo(MAX_RANGE) > 0) {
			throw new InvalidCalendarRangeException("Intervalo inválido: até %d dias.".formatted(MAX_RANGE.toDays()));
		}
		Optional<String> accessToken = accessTokens.current();
		if (accessToken.isEmpty()) {
			return List.of();
		}
		Set<String> ownEvents = linkRepository.findAll().stream()
				.map(GoogleEventLinkEntity::getGoogleEventId)
				.collect(Collectors.toSet());
		return gateway.listEvents(accessToken.get(), from, to).stream()
				.filter(item -> !ownEvents.contains(item.id()))
				.toList();
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
