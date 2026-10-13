package com.diegohaefliger.atsresumeoptimizer.googlecalendar.application;

import com.diegohaefliger.atsresumeoptimizer.ai.SecretCipher;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleCredentials;
import com.github.f4b6a3.uuid.UuidCreator;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
class GoogleCredentialsStore {

	private final GoogleClientSettingsRepository repository;
	private final GoogleCalendarProperties properties;
	private final SecretCipher cipher;
	private final Clock clock;

	GoogleCredentialsStore(GoogleClientSettingsRepository repository, GoogleCalendarProperties properties,
			SecretCipher cipher, Clock clock) {
		this.repository = repository;
		this.properties = properties;
		this.cipher = cipher;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	Optional<GoogleCredentials> current() {
		Optional<GoogleCredentials> saved = repository.findFirstByOrderByUpdatedAtDesc()
				.map(settings -> new GoogleCredentials(settings.getClientId(), cipher.decrypt(settings.getClientSecret())));
		if (saved.isPresent() || !properties.configured()) {
			return saved;
		}
		return Optional.of(new GoogleCredentials(properties.clientId(), properties.clientSecret()));
	}

	@Transactional
	void save(GoogleCredentials credentials) {
		GoogleClientSettingsEntity settings = repository.findFirstByOrderByUpdatedAtDesc()
				.orElseGet(() -> new GoogleClientSettingsEntity(UuidCreator.getTimeOrderedEpoch()));
		settings.setClientId(credentials.clientId());
		settings.setClientSecret(cipher.encrypt(credentials.clientSecret()));
		settings.setUpdatedAt(Instant.now(clock));
		repository.save(settings);
	}

	@Transactional
	void clear() {
		repository.deleteAll();
	}
}
