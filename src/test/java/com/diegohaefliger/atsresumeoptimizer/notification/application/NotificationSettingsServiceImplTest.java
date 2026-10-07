package com.diegohaefliger.atsresumeoptimizer.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.notification.domain.InvalidNotificationSettingsException;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationChannel;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationSettings;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationSettingsServiceImplTest {

	private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

	@Mock
	private NotificationSettingsRepository repository;

	private NotificationSettingsServiceImpl service() {
		return new NotificationSettingsServiceImpl(repository, new NotificationSettingsEntityMapperImpl(),
				Clock.fixed(Instant.parse("2026-10-06T12:00:00Z"), ZoneOffset.UTC));
	}

	private NotificationSettings settings(List<Integer> leads, Set<NotificationChannel> channels) {
		return new NotificationSettings(leads, channels, ZONE);
	}

	@Test
	void fallsBackToTheDefaultWhenNothingWasSaved() {
		when(repository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.empty());

		assertThat(service().current()).isEqualTo(NotificationSettings.DEFAULT);
	}

	@Test
	void savesAndReturnsTheLeadTimesSorted() {
		when(repository.findFirstByOrderByUpdatedAtDesc()).thenReturn(Optional.empty());

		NotificationSettings saved = service().save(settings(List.of(1440, 15, 60), Set.of(NotificationChannel.IN_APP)));

		assertThat(saved.leadMinutes()).containsExactly(15, 60, 1440);
		assertThat(saved.channels()).containsExactly(NotificationChannel.IN_APP);
		assertThat(saved.timezone()).isEqualTo(ZONE);
		verify(repository).save(any());
	}

	@Test
	void refusesInvalidSettings() {
		Set<NotificationChannel> inApp = Set.of(NotificationChannel.IN_APP);

		assertThatThrownBy(() -> service().save(settings(List.of(), inApp)))
				.isInstanceOf(InvalidNotificationSettingsException.class);
		assertThatThrownBy(() -> service().save(settings(List.of(0), inApp)))
				.isInstanceOf(InvalidNotificationSettingsException.class);
		assertThatThrownBy(() -> service().save(settings(List.of(60, 60), inApp)))
				.isInstanceOf(InvalidNotificationSettingsException.class);
		assertThatThrownBy(() -> service().save(settings(List.of(1, 2, 3, 4, 5, 6), inApp)))
				.isInstanceOf(InvalidNotificationSettingsException.class);
		assertThatThrownBy(() -> service().save(settings(List.of(60), Set.of())))
				.isInstanceOf(InvalidNotificationSettingsException.class);
		assertThatThrownBy(() -> service().save(settings(List.of(60), Set.of(NotificationChannel.EMAIL))))
				.isInstanceOf(InvalidNotificationSettingsException.class)
				.hasMessageContaining("EMAIL");

		verify(repository, never()).save(any());
	}
}
