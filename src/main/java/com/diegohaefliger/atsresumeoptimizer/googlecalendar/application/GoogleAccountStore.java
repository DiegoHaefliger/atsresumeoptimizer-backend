package com.diegohaefliger.atsresumeoptimizer.googlecalendar.application;

import com.diegohaefliger.atsresumeoptimizer.ai.SecretCipher;
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
	void saveConnection(String refreshToken, String accountEmail) {
		GoogleAccountEntity account = repository.findFirstByOrderByUpdatedAtDesc()
				.orElseGet(() -> new GoogleAccountEntity(UuidCreator.getTimeOrderedEpoch()));
		account.setRefreshToken(cipher.encrypt(refreshToken));
		account.setAccountEmail(accountEmail);
		account.setUpdatedAt(Instant.now(clock));
		repository.save(account);
	}

	@Transactional
	void clearConnection() {
		repository.deleteAll();
	}
}
