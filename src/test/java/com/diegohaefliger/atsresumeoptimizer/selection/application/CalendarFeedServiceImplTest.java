package com.diegohaefliger.atsresumeoptimizer.selection.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CalendarFeedServiceImplTest {

	@Mock
	private CalendarFeedRepository repository;

	private CalendarFeedServiceImpl service() {
		return new CalendarFeedServiceImpl(repository, Clock.fixed(Instant.parse("2026-10-06T12:00:00Z"), ZoneOffset.UTC));
	}

	@Test
	void generatesALongRandomTokenAndReplacesTheOldOne() {
		CalendarFeedEntity existing = new CalendarFeedEntity(UUID.randomUUID());
		existing.setToken("antigo");
		when(repository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.of(existing));
		when(repository.save(any())).thenAnswer(call -> call.getArgument(0));

		String first = service().regenerate();
		String second = service().regenerate();

		assertThat(first).hasSize(64).matches("[0-9a-f]+").isNotEqualTo("antigo");
		assertThat(second).isNotEqualTo(first);
	}

	@Test
	void acceptsOnlyTheCurrentToken() {
		CalendarFeedEntity entity = new CalendarFeedEntity(UUID.randomUUID());
		entity.setToken("segredo");
		when(repository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.of(entity));

		assertThat(service().accepts("segredo")).isTrue();
		assertThat(service().accepts("errado")).isFalse();
	}

	@Test
	void refusesEverythingWhenTheFeedIsDisabled() {
		when(repository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.empty());

		assertThat(service().token()).isEmpty();
		assertThat(service().accepts("")).isFalse();
	}

	@Test
	void disablingRemovesTheToken() {
		service().disable();

		verify(repository).deleteAll();
	}
}
