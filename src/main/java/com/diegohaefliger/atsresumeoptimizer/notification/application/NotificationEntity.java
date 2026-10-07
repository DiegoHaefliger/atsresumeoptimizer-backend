package com.diegohaefliger.atsresumeoptimizer.notification.application;

import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationType;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification")
class NotificationEntity {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	private NotificationType type;

	private String title;

	private String message;

	@Column(name = "process_id")
	private UUID processId;

	@Column(name = "schedule_id")
	private UUID scheduleId;

	@Column(name = "lead_minutes")
	private Integer leadMinutes;

	@Column(name = "created_at")
	private Instant createdAt;

	@Column(name = "read_at")
	private Instant readAt;

	protected NotificationEntity() {
	}

	NotificationEntity(NotificationType type, String title, String message, UUID processId, UUID scheduleId,
			Integer leadMinutes, Instant createdAt) {
		this.id = UuidCreator.getTimeOrderedEpoch();
		this.type = type;
		this.title = title;
		this.message = message;
		this.processId = processId;
		this.scheduleId = scheduleId;
		this.leadMinutes = leadMinutes;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public NotificationType getType() {
		return type;
	}

	public String getTitle() {
		return title;
	}

	public String getMessage() {
		return message;
	}

	public UUID getProcessId() {
		return processId;
	}

	public UUID getScheduleId() {
		return scheduleId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getReadAt() {
		return readAt;
	}

	public void setReadAt(Instant readAt) {
		this.readAt = readAt;
	}
}
