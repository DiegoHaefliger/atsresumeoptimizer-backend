package com.diegohaefliger.atsresumeoptimizer.ai.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ai_provider_config")
class AiProviderConfigEntity {

	@Id
	@Enumerated(EnumType.STRING)
	private AiProvider provider;

	@Column(name = "api_key_encrypted")
	private String apiKeyEncrypted;

	@Column(name = "api_key_hint")
	private String apiKeyHint;

	@Column(name = "base_url")
	private String baseUrl;

	private String model;

	@Column(name = "heavy_model")
	private String heavyModel;

	private boolean enabled;

	private Integer priority;

	@Column(name = "updated_at")
	private Instant updatedAt;

	protected AiProviderConfigEntity() {
	}

	AiProviderConfigEntity(AiProvider provider) {
		this.provider = provider;
	}

	void update(String baseUrl, String model, String heavyModel) {
		this.baseUrl = baseUrl;
		this.model = model;
		this.heavyModel = heavyModel;
		this.updatedAt = Instant.now();
	}

	void enable(int priority) {
		this.enabled = true;
		this.priority = priority;
	}

	void disable() {
		this.enabled = false;
		this.priority = null;
	}

	void replaceApiKey(String apiKeyEncrypted, String apiKeyHint) {
		this.apiKeyEncrypted = apiKeyEncrypted;
		this.apiKeyHint = apiKeyHint;
	}

	AiProvider provider() {
		return provider;
	}

	String apiKeyEncrypted() {
		return apiKeyEncrypted;
	}

	String apiKeyHint() {
		return apiKeyHint;
	}

	boolean enabled() {
		return enabled;
	}

	Integer priority() {
		return priority;
	}

	String baseUrl() {
		return baseUrl;
	}

	String model() {
		return model;
	}

	String heavyModel() {
		return heavyModel;
	}
}
