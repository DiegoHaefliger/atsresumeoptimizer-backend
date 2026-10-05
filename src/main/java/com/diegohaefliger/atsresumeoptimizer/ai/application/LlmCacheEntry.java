package com.diegohaefliger.atsresumeoptimizer.ai.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "llm_cache")
class LlmCacheEntry {

	@Id
	@Column(name = "key_hash")
	private String keyHash;

	@Column(name = "prompt_key")
	private String promptKey;

	@Column(name = "prompt_version")
	private int promptVersion;

	@JdbcTypeCode(SqlTypes.JSON)
	private String response;

	@Column(name = "created_at")
	private Instant createdAt;

	protected LlmCacheEntry() {
	}

	LlmCacheEntry(String keyHash, String promptKey, int promptVersion, String response) {
		this.keyHash = keyHash;
		this.promptKey = promptKey;
		this.promptVersion = promptVersion;
		this.response = response;
		this.createdAt = Instant.now();
	}

	String response() {
		return response;
	}
}
