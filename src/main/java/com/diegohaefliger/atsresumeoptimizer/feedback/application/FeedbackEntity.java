package com.diegohaefliger.atsresumeoptimizer.feedback.application;

import com.diegohaefliger.atsresumeoptimizer.feedback.domain.FeedbackSubmission;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "feedback")
class FeedbackEntity {

	@Id
	private UUID id;

	private int rating;

	private String message;

	private String contact;

	@Column(name = "allow_publish")
	private boolean allowPublish;

	@Column(name = "app_version")
	private String appVersion;

	@Column(name = "created_at")
	private Instant createdAt;

	@Column(name = "delivered_at")
	private Instant deliveredAt;

	protected FeedbackEntity() {
	}

	FeedbackEntity(UUID id, Instant createdAt, FeedbackSubmission submission) {
		this.id = id;
		this.createdAt = createdAt;
		this.rating = submission.rating();
		this.message = submission.message();
		this.contact = submission.contact();
		this.allowPublish = submission.allowPublish();
		this.appVersion = submission.appVersion();
	}

	UUID getId() {
		return id;
	}

	public int getRating() {
		return rating;
	}

	public String getMessage() {
		return message;
	}

	public String getContact() {
		return contact;
	}

	public boolean isAllowPublish() {
		return allowPublish;
	}

	public String getAppVersion() {
		return appVersion;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	boolean isDelivered() {
		return deliveredAt != null;
	}

	void markDelivered(Instant now) {
		this.deliveredAt = now;
	}
}
