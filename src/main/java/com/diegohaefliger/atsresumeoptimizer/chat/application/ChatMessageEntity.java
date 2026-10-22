package com.diegohaefliger.atsresumeoptimizer.chat.application;

import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatRole;
import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_message")
class ChatMessageEntity {

	@Id
	private UUID id;

	private UUID conversationId;

	@Enumerated(EnumType.STRING)
	private ChatRole role;

	private String content;

	private String aiModel;

	private int tokensIn;

	private int tokensOut;

	private BigDecimal costUsd;

	private boolean interrupted;

	private Instant createdAt;

	protected ChatMessageEntity() {
	}

	ChatMessageEntity(UUID conversationId, ChatRole role, String content, Instant now) {
		this.id = UuidCreator.getTimeOrderedEpoch();
		this.conversationId = conversationId;
		this.role = role;
		this.content = content;
		this.costUsd = BigDecimal.ZERO;
		this.createdAt = now;
	}

	public UUID getId() {
		return id;
	}

	public ChatRole getRole() {
		return role;
	}

	public String getContent() {
		return content;
	}

	public String getAiModel() {
		return aiModel;
	}

	public int getTokensIn() {
		return tokensIn;
	}

	public int getTokensOut() {
		return tokensOut;
	}

	public BigDecimal getCostUsd() {
		return costUsd;
	}

	public boolean isInterrupted() {
		return interrupted;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
