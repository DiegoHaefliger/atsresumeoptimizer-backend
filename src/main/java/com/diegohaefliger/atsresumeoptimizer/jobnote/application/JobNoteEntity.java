package com.diegohaefliger.atsresumeoptimizer.jobnote.application;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "job_note")
class JobNoteEntity {

	@Id
	private UUID id;

	@Column(name = "job_posting_id")
	private UUID jobPostingId;

	private String content;

	@Column(name = "updated_at")
	private Instant updatedAt;

	protected JobNoteEntity() {
	}

	JobNoteEntity(UUID jobPostingId, String content, Instant now) {
		this.id = UuidCreator.getTimeOrderedEpoch();
		this.jobPostingId = jobPostingId;
		this.content = content;
		this.updatedAt = now;
	}

	public UUID getJobPostingId() {
		return jobPostingId;
	}

	public String getContent() {
		return content;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	void replace(String content, Instant now) {
		this.content = content;
		this.updatedAt = now;
	}
}
