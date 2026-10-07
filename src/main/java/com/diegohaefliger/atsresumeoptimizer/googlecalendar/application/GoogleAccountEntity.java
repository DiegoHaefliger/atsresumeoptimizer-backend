package com.diegohaefliger.atsresumeoptimizer.googlecalendar.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "google_account")
class GoogleAccountEntity {

	@Id
	private UUID id;

	@Column(name = "refresh_token")
	private String refreshToken;

	@Column(name = "account_email")
	private String accountEmail;

	@Column(name = "updated_at")
	private Instant updatedAt;

	protected GoogleAccountEntity() {
	}

	GoogleAccountEntity(UUID id) {
		this.id = id;
	}

	public String getRefreshToken() {
		return refreshToken;
	}

	public void setRefreshToken(String refreshToken) {
		this.refreshToken = refreshToken;
	}

	public String getAccountEmail() {
		return accountEmail;
	}

	public void setAccountEmail(String accountEmail) {
		this.accountEmail = accountEmail;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
