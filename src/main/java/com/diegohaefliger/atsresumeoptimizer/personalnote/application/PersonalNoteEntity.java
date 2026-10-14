package com.diegohaefliger.atsresumeoptimizer.personalnote.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "personal_note")
class PersonalNoteEntity {

	static final UUID SINGLE_ID = UUID.fromString("00000000-0000-7000-8000-000000000001");

	@Id
	private UUID id;

	private String content;

	@Column(name = "updated_at")
	private Instant updatedAt;

	protected PersonalNoteEntity() {
	}

	PersonalNoteEntity(String content, Instant now) {
		this.id = SINGLE_ID;
		this.content = content;
		this.updatedAt = now;
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
