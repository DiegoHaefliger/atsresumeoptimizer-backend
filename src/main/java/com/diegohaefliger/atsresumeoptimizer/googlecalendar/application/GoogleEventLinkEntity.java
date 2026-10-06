package com.diegohaefliger.atsresumeoptimizer.googlecalendar.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "google_event_link")
class GoogleEventLinkEntity {

	@Id
	@Column(name = "schedule_id")
	private UUID scheduleId;

	@Column(name = "google_event_id")
	private String googleEventId;

	@Column(name = "synced_at")
	private Instant syncedAt;

	protected GoogleEventLinkEntity() {
	}

	GoogleEventLinkEntity(UUID scheduleId, String googleEventId, Instant syncedAt) {
		this.scheduleId = scheduleId;
		this.googleEventId = googleEventId;
		this.syncedAt = syncedAt;
	}

	public String getGoogleEventId() {
		return googleEventId;
	}

	public void setGoogleEventId(String googleEventId) {
		this.googleEventId = googleEventId;
	}

	public void setSyncedAt(Instant syncedAt) {
		this.syncedAt = syncedAt;
	}
}
