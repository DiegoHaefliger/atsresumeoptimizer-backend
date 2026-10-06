package com.diegohaefliger.atsresumeoptimizer.selection.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "calendar_feed")
class CalendarFeedEntity {

	@Id
	private UUID id;

	private String token;

	@Column(name = "updated_at")
	private Instant updatedAt;

	protected CalendarFeedEntity() {
	}

	CalendarFeedEntity(UUID id) {
		this.id = id;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
