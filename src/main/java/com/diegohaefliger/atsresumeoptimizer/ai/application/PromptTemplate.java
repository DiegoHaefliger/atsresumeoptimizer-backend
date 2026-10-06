package com.diegohaefliger.atsresumeoptimizer.ai.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "prompt_template")
class PromptTemplate {

	@Id
	private UUID id;

	@Column(name = "key")
	private String key;

	private int version;

	private String content;

	private String model;

	private boolean active;

	@Column(name = "created_at")
	private Instant createdAt;

	protected PromptTemplate() {
	}

	PromptTemplate(UUID id, String key, int version, String content, String model) {
		this.id = id;
		this.key = key;
		this.version = version;
		this.content = content;
		this.model = model;
		this.active = true;
		this.createdAt = Instant.now();
	}

	public String getKey() {
		return key;
	}

	public int getVersion() {
		return version;
	}

	public String getContent() {
		return content;
	}

	public String getModel() {
		return model;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
