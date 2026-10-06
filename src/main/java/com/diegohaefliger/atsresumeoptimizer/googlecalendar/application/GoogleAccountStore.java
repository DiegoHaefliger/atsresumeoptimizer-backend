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
class GoogleAccountStore {

	private final GoogleAccountRepository repository;
	private final SecretCipher cipher;
	private final Clock clock;

	GoogleAccountStore(GoogleAccountRepository repository, SecretCipher cipher, Clock clock) {
		this.repository = repository;
		this.cipher = cipher;
		this.clock = clock;
	}

	@Transactional(readOnly = true)
	Optional<GoogleCredentials> credentials() {
		return repository.findFirstByOrderByUpdatedAtDesc()
				.map(account -> new GoogleCredentials(account.getClientId(), cipher.decrypt(account.getClientSecret())));
	}

	@Transactional(readOnly = true)
	Optional<String> refreshToken() {
		return repository.findFirstByOrderByUpdatedAtDesc()
				.map(GoogleAccountEntity::getRefreshToken)
				.map(cipher::decrypt);
	}

	@Transactional(readOnly = true)
	Optional<GoogleAccountEntity> account() {
		return repository.findFirstByOrderByUpdatedAtDesc();
	}

	@Transactional
	void saveCredentials(GoogleCredentials credentials) {
		GoogleAccountEntity account = repository.findFirstByOrderByUpdatedAtDesc()
				.orElseGet(() -> new GoogleAccountEntity(UuidCreator.getTimeOrderedEpoch()));
		boolean changed = !credentials.clientId().equals(account.getClientId());
		account.setClientId(credentials.clientId());
		account.setClientSecret(cipher.encrypt(credentials.clientSecret()));
		if (changed) {
			account.setRefreshToken(null);
			account.setAccountEmail(null);
		}
		touch(account);
	}

	@Transactional
	void saveConnection(String refreshToken, String accountEmail) {
		GoogleAccountEntity account = repository.findFirstByOrderByUpdatedAtDesc().orElseThrow();
		account.setRefreshToken(cipher.encrypt(refreshToken));
		account.setAccountEmail(accountEmail);
		touch(account);
	}

	@Transactional
	void clearConnection() {
		repository.findFirstByOrderByUpdatedAtDesc().ifPresent(account -> {
			account.setRefreshToken(null);
			account.setAccountEmail(null);
			touch(account);
		});
	}

	@Transactional
	void clearAll() {
		repository.deleteAll();
	}

	private void touch(GoogleAccountEntity account) {
		account.setUpdatedAt(Instant.now(clock));
		repository.save(account);
	}
}
