package com.diegohaefliger.atsresumeoptimizer.notification.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationType;
import java.time.Instant;
import java.util.UUID;

record NotificationResponse(
		UUID id,
		NotificationType type,
		String title,
		String message,
		UUID processId,
		UUID scheduleId,
		Instant createdAt,
		Instant readAt) {
}
