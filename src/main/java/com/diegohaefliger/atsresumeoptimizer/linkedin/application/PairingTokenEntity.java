package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "linkedin_pairing")
class PairingTokenEntity {

	@Id
	private UUID id;

	private String token;

	@Column(name = "created_at")
	private Instant createdAt;

	protected PairingTokenEntity() {
	}

	PairingTokenEntity(UUID id, String token) {
		this.id = id;
		this.token = token;
		this.createdAt = Instant.now();
	}

	String token() {
		return token;
	}

	void replaceToken(String token) {
		this.token = token;
		this.createdAt = Instant.now();
	}
}
