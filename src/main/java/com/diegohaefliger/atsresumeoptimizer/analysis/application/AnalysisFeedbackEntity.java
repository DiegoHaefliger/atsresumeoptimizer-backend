package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analysis_feedback")
class AnalysisFeedbackEntity {

	@Id
	private UUID id;

	@Column(name = "analysis_id")
	private UUID analysisId;

	private int rating;

	private String comment;

	@Column(name = "created_at")
	private Instant createdAt;

	protected AnalysisFeedbackEntity() {
	}

	AnalysisFeedbackEntity(UUID id, UUID analysisId, int rating, String comment) {
		this.id = id;
		this.analysisId = analysisId;
		this.rating = rating;
		this.comment = comment;
		this.createdAt = Instant.now();
	}
}
