package com.diegohaefliger.atsresumeoptimizer.selection.application;

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
@Table(name = "selection_stage_movement")
class SelectionStageMovementEntity {

	@Id
	private UUID id;

	@Column(name = "process_id")
	private UUID processId;

	@Enumerated(EnumType.STRING)
	private SelectionStage stage;

	private String note;

	@Column(name = "moved_at")
	private Instant movedAt;

	protected SelectionStageMovementEntity() {
	}

	SelectionStageMovementEntity(UUID processId, SelectionStage stage, String note, Instant movedAt) {
		this.id = UuidCreator.getTimeOrderedEpoch();
		this.processId = processId;
		this.stage = stage;
		this.note = note;
		this.movedAt = movedAt;
	}

	public SelectionStage getStage() {
		return stage;
	}

	public String getNote() {
		return note;
	}

	public Instant getMovedAt() {
		return movedAt;
	}
}
