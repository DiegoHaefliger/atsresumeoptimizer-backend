package com.diegohaefliger.atsresumeoptimizer.chat.application;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_conversation")
class ChatConversationEntity {

	@Id
	private UUID id;

	private String title;

	private Instant createdAt;

	private Instant updatedAt;

	protected ChatConversationEntity() {
	}

	ChatConversationEntity(String title, Instant now) {
		this.id = UuidCreator.getTimeOrderedEpoch();
		this.title = title;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public UUID getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	void rename(String title, Instant now) {
		this.title = title;
		this.updatedAt = now;
	}
}
