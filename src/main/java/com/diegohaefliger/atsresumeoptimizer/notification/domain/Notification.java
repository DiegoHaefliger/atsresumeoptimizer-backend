package com.diegohaefliger.atsresumeoptimizer.notification.domain;

import java.time.Instant;
import java.util.UUID;

public record Notification(
		UUID id,
		NotificationType type,
		String title,
		String message,
		UUID processId,
		UUID scheduleId,
		Instant createdAt,
		Instant readAt) {
}
