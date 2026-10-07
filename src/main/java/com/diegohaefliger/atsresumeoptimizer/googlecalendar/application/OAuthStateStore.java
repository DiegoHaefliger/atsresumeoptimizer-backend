package com.diegohaefliger.atsresumeoptimizer.googlecalendar.application;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
class OAuthStateStore {

	private static final Duration VALIDITY = Duration.ofMinutes(10);
	private static final int STATE_BYTES = 24;

	private final Map<String, Instant> pending = new ConcurrentHashMap<>();
	private final SecureRandom random = new SecureRandom();
	private final Clock clock;

	OAuthStateStore(Clock clock) {
		this.clock = clock;
	}

	String issue() {
		Instant now = Instant.now(clock);
		pending.values().removeIf(expiresAt -> expiresAt.isBefore(now));
		byte[] bytes = new byte[STATE_BYTES];
		random.nextBytes(bytes);
		String state = HexFormat.of().formatHex(bytes);
		pending.put(state, now.plus(VALIDITY));
		return state;
	}

	boolean consume(String state) {
		if (state == null) {
			return false;
		}
		Instant expiresAt = pending.remove(state);
		return expiresAt != null && !expiresAt.isBefore(Instant.now(clock));
	}
}
