package com.diegohaefliger.atsresumeoptimizer.selection.application;

import com.github.f4b6a3.uuid.UuidCreator;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CalendarFeedServiceImpl implements CalendarFeedService {

	private static final int TOKEN_BYTES = 32;

	private final CalendarFeedRepository repository;
	private final Clock clock;
	private final SecureRandom random = new SecureRandom();

	CalendarFeedServiceImpl(CalendarFeedRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<String> token() {
		return repository.findFirstByOrderByUpdatedAtDesc().map(CalendarFeedEntity::getToken);
	}

	@Override
	@Transactional
	public String regenerate() {
		CalendarFeedEntity entity = repository.findFirstByOrderByUpdatedAtDesc()
				.orElseGet(() -> new CalendarFeedEntity(UuidCreator.getTimeOrderedEpoch()));
		byte[] bytes = new byte[TOKEN_BYTES];
		random.nextBytes(bytes);
		entity.setToken(HexFormat.of().formatHex(bytes));
		entity.setUpdatedAt(Instant.now(clock));
		return repository.save(entity).getToken();
	}

	@Override
	@Transactional
	public void disable() {
		repository.deleteAll();
	}

	@Override
	@Transactional(readOnly = true)
	public boolean accepts(String candidate) {
		return token()
				.map(token -> MessageDigest.isEqual(
						token.getBytes(StandardCharsets.UTF_8), candidate.getBytes(StandardCharsets.UTF_8)))
				.orElse(false);
	}
}
