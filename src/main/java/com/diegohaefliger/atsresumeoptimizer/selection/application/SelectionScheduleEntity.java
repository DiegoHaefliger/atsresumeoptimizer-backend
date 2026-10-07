package com.diegohaefliger.atsresumeoptimizer.selection.application;

import com.diegohaefliger.atsresumeoptimizer.selection.ScheduleStatus;
import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
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
@Table(name = "selection_schedule")
class SelectionScheduleEntity {

	@Id
	private UUID id;

	@Column(name = "process_id")
	private UUID processId;

	@Enumerated(EnumType.STRING)
	private SelectionStage stage;

	@Column(name = "scheduled_at")
	private Instant scheduledAt;

	@Column(name = "duration_minutes")
	private Integer durationMinutes;

	private String location;

	private String notes;

	@Enumerated(EnumType.STRING)
	private ScheduleStatus status;

	@Column(name = "created_at")
	private Instant createdAt;

	@Column(name = "updated_at")
	private Instant updatedAt;

	protected SelectionScheduleEntity() {
	}

	SelectionScheduleEntity(UUID processId, SelectionStage stage, Instant now) {
		this.id = UuidCreator.getTimeOrderedEpoch();
		this.processId = processId;
		this.stage = stage;
		this.status = ScheduleStatus.SCHEDULED;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public UUID getId() {
		return id;
	}

	public UUID getProcessId() {
		return processId;
	}

	public SelectionStage getStage() {
		return stage;
	}

	public Instant getScheduledAt() {
		return scheduledAt;
	}

	public void setScheduledAt(Instant scheduledAt) {
		this.scheduledAt = scheduledAt;
	}

	public Integer getDurationMinutes() {
		return durationMinutes;
	}

	public void setDurationMinutes(Integer durationMinutes) {
		this.durationMinutes = durationMinutes;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public ScheduleStatus getStatus() {
		return status;
	}

	public void setStatus(ScheduleStatus status) {
		this.status = status;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
