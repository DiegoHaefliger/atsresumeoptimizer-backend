package com.diegohaefliger.atsresumeoptimizer.notification.domain;

import java.util.UUID;

public record NotificationDraft(
		NotificationType type, String title, String message, UUID processId, UUID scheduleId, Integer leadMinutes) {
}
