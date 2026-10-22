package com.diegohaefliger.atsresumeoptimizer.chat.application;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_conversation_job")
class ChatAttachmentEntity {

	@Id
	private UUID id;

	private UUID conversationId;

	private UUID jobPostingId;

	private UUID resumeVersionId;

	private Instant createdAt;

	protected ChatAttachmentEntity() {
	}

	ChatAttachmentEntity(UUID conversationId, UUID jobPostingId, UUID resumeVersionId, Instant now) {
		this.id = UuidCreator.getTimeOrderedEpoch();
		this.conversationId = conversationId;
		this.jobPostingId = jobPostingId;
		this.resumeVersionId = resumeVersionId;
		this.createdAt = now;
	}

	public UUID getJobPostingId() {
		return jobPostingId;
	}

	public UUID getResumeVersionId() {
		return resumeVersionId;
	}

	void useResumeVersion(UUID resumeVersionId) {
		this.resumeVersionId = resumeVersionId;
	}
}
