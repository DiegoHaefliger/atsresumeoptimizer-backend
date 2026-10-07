package com.diegohaefliger.atsresumeoptimizer.googlecalendar.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class OAuthStateStoreTest {

	private static final Instant NOW = Instant.parse("2026-10-06T12:00:00Z");

	@Test
	void aStateWorksOnlyOnce() {
		OAuthStateStore store = new OAuthStateStore(Clock.fixed(NOW, ZoneOffset.UTC));
		String state = store.issue();

		assertThat(store.consume(state)).isTrue();
		assertThat(store.consume(state)).isFalse();
	}

	@Test
	void refusesUnknownNullAndExpiredStates() {
		OAuthStateStore issuing = new OAuthStateStore(Clock.fixed(NOW, ZoneOffset.UTC));
		String state = issuing.issue();

		assertThat(issuing.consume("outro")).isFalse();
		assertThat(issuing.consume(null)).isFalse();
		assertThat(new OAuthStateStore(Clock.fixed(NOW.plus(Duration.ofHours(1)), ZoneOffset.UTC)).consume(state)).isFalse();
	}
}
