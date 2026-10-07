package com.diegohaefliger.atsresumeoptimizer.notification.application;

import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationChannel;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationDraft;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
class InAppNotificationSender implements NotificationSender {

	private final NotificationRepository repository;
	private final Clock clock;

	InAppNotificationSender(NotificationRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	@Override
	public NotificationChannel channel() {
		return NotificationChannel.IN_APP;
	}

	@Override
	public void send(NotificationDraft draft) {
		repository.save(new NotificationEntity(draft.type(), draft.title(), draft.message(), draft.processId(),
				draft.scheduleId(), draft.leadMinutes(), Instant.now(clock)));
	}
}
