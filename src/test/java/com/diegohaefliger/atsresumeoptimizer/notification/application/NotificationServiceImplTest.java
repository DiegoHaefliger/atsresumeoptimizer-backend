package com.diegohaefliger.atsresumeoptimizer.notification.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.notification.domain.Notification;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

	private static final Instant NOW = Instant.parse("2026-10-06T12:00:00Z");

	@Mock
	private NotificationRepository repository;

	private NotificationServiceImpl service() {
		return new NotificationServiceImpl(repository, new NotificationEntityMapperImpl(), Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private NotificationEntity entity() {
		return new NotificationEntity(NotificationType.SCHEDULE_REMINDER, "Lembrete", "Acme", UUID.randomUUID(),
				UUID.randomUUID(), 60, NOW.minusSeconds(60));
	}

	@Test
	void listsAllOrOnlyUnread() {
		when(repository.findAllByOrderByCreatedAtDesc(Limit.of(10))).thenReturn(List.of(entity(), entity()));
		when(repository.findByReadAtIsNullOrderByCreatedAtDesc(Limit.of(10))).thenReturn(List.of(entity()));

		assertThat(service().list(false, 10)).hasSize(2);
		assertThat(service().list(true, 10)).hasSize(1);
	}

	@Test
	void countsUnread() {
		when(repository.countByReadAtIsNull()).thenReturn(3L);

		assertThat(service().unreadCount()).isEqualTo(3L);
	}

	@Test
	void marksAsReadOnceKeepingTheFirstReadTime() {
		NotificationEntity entity = entity();
		when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));

		Notification read = service().markRead(entity.getId());
		assertThat(read.readAt()).isEqualTo(NOW);

		NotificationServiceImpl later = new NotificationServiceImpl(repository, new NotificationEntityMapperImpl(),
				Clock.fixed(NOW.plusSeconds(600), ZoneOffset.UTC));
		assertThat(later.markRead(entity.getId()).readAt()).isEqualTo(NOW);
	}

	@Test
	void marksEverythingAsRead() {
		when(repository.markAllRead(NOW)).thenReturn(4);

		assertThat(service().markAllRead()).isEqualTo(4);
	}

	@Test
	void deletesAndRefusesUnknownIds() {
		NotificationEntity entity = entity();
		UUID unknown = UUID.randomUUID();
		when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));
		when(repository.findById(unknown)).thenReturn(Optional.empty());

		service().delete(entity.getId());
		verify(repository).delete(entity);
		assertThatThrownBy(() -> service().delete(unknown)).isInstanceOf(NotificationNotFoundException.class);
		assertThatThrownBy(() -> service().markRead(unknown)).isInstanceOf(NotificationNotFoundException.class);
	}
}
