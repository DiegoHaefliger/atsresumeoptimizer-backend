package com.diegohaefliger.atsresumeoptimizer.notification.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "notification_settings")
class NotificationSettingsEntity {

	@Id
	private UUID id;

	@JdbcTypeCode(SqlTypes.ARRAY)
	@Column(name = "lead_minutes")
	private List<Integer> leadMinutes;

	@JdbcTypeCode(SqlTypes.ARRAY)
	private List<String> channels;

	private String timezone;

	@Column(name = "updated_at")
	private Instant updatedAt;

	protected NotificationSettingsEntity() {
	}

	NotificationSettingsEntity(UUID id) {
		this.id = id;
	}

	public List<Integer> getLeadMinutes() {
		return leadMinutes;
	}

	public void setLeadMinutes(List<Integer> leadMinutes) {
		this.leadMinutes = leadMinutes;
	}

	public List<String> getChannels() {
		return channels;
	}

	public void setChannels(List<String> channels) {
		this.channels = channels;
	}

	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
