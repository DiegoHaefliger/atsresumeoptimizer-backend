package com.diegohaefliger.atsresumeoptimizer.ai.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_settings")
class AiSettingsEntity {

	@Id
	private UUID id;

	private BigDecimal temperature;

	@Column(name = "max_output_tokens")
	private int maxOutputTokens;

	@Column(name = "timeout_seconds")
	private int timeoutSeconds;

	@Column(name = "updated_at")
	private Instant updatedAt;

	protected AiSettingsEntity() {
	}

	AiSettingsEntity(UUID id) {
		this.id = id;
	}

	void update(BigDecimal temperature, int maxOutputTokens, int timeoutSeconds) {
		this.temperature = temperature;
		this.maxOutputTokens = maxOutputTokens;
		this.timeoutSeconds = timeoutSeconds;
		this.updatedAt = Instant.now();
	}

	BigDecimal temperature() {
		return temperature;
	}

	int maxOutputTokens() {
		return maxOutputTokens;
	}

	int timeoutSeconds() {
		return timeoutSeconds;
	}
}
