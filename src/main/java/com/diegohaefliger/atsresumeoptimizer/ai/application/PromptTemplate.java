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

	String key() {
		return key;
	}

	int version() {
		return version;
	}

	String content() {
		return content;
	}
}
