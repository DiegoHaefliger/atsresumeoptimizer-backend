package com.diegohaefliger.atsresumeoptimizer.jobsearch.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.RunTrigger;

@Entity
@Table(name = "job_search_run")
class SearchRunEntity {

	@Id
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(name = "triggered_by")
	private RunTrigger trigger;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "finished_at")
	private Instant finishedAt;

	@Column(name = "read_count")
	private int read;

	@Column(name = "added_count")
	private int added;

	@Column(name = "duplicate_count")
	private int duplicates;

	@Column(name = "below_adherence_count")
	private int belowAdherence;

	private String error;

	protected SearchRunEntity() {
	}

	SearchRunEntity(UUID id, RunTrigger trigger, Instant startedAt, Instant finishedAt, int read, int added,
			int duplicates, int belowAdherence, String error) {
		this.id = id;
		this.trigger = trigger;
		this.startedAt = startedAt;
		this.finishedAt = finishedAt;
		this.read = read;
		this.added = added;
		this.duplicates = duplicates;
		this.belowAdherence = belowAdherence;
		this.error = error;
	}

	public UUID getId() {
		return id;
	}

	public RunTrigger getTrigger() {
		return trigger;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getFinishedAt() {
		return finishedAt;
	}

	public int getRead() {
		return read;
	}

	public int getAdded() {
		return added;
	}

	public int getDuplicates() {
		return duplicates;
	}

	public int getBelowAdherence() {
		return belowAdherence;
	}

	public String getError() {
		return error;
	}
}
