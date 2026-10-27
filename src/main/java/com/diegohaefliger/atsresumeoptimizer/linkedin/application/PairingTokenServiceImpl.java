package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.InvalidPairingTokenException;
import com.github.f4b6a3.uuid.UuidCreator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class PairingTokenServiceImpl implements PairingTokenService {

	private static final int TOKEN_BYTES = 32;

	private final PairingTokenRepository repository;
	private final SecureRandom random = new SecureRandom();

	PairingTokenServiceImpl(PairingTokenRepository repository) {
		this.repository = repository;
	}

	@Override
	@Transactional
	public String current() {
		return repository.findFirstByOrderByCreatedAtDesc().orElseGet(this::create).token();
	}

	@Override
	@Transactional
	public String regenerate() {
		PairingTokenEntity entity = repository.findFirstByOrderByCreatedAtDesc().orElseGet(this::create);
		entity.replaceToken(newToken());
		return repository.save(entity).token();
	}

	@Override
	@Transactional(readOnly = true)
	public void verify(String token) {
		String expected = repository.findFirstByOrderByCreatedAtDesc().map(PairingTokenEntity::token).orElse(null);
		if (token == null || expected == null || !MessageDigest.isEqual(
				expected.getBytes(StandardCharsets.UTF_8), token.strip().getBytes(StandardCharsets.UTF_8))) {
			throw new InvalidPairingTokenException();
		}
	}

	private PairingTokenEntity create() {
		return repository.save(new PairingTokenEntity(UuidCreator.getTimeOrderedEpoch(), newToken()));
	}

	private String newToken() {
		byte[] bytes = new byte[TOKEN_BYTES];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}
}
