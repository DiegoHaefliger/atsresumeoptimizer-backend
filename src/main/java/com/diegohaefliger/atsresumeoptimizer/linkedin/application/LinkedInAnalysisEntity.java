package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInAnalysisReport;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "linkedin_analysis")
class LinkedInAnalysisEntity {

	@Id
	private UUID id;

	@Column(name = "snapshot_id")
	private UUID snapshotId;

	@Column(name = "created_at")
	private Instant createdAt;

	@Column(name = "overall_score")
	private int overallScore;

	private String summary;

	@Convert(converter = LinkedInReportConverter.class)
	private LinkedInAnalysisReport report;

	@Column(name = "images_analyzed")
	private boolean imagesAnalyzed;

	@Column(name = "ai_model")
	private String aiModel;

	@Column(name = "tokens_in")
	private Integer tokensIn;

	@Column(name = "tokens_out")
	private Integer tokensOut;

	@Column(name = "cost_usd")
	private BigDecimal costUsd;

	protected LinkedInAnalysisEntity() {
	}

	LinkedInAnalysisEntity(UUID id, UUID snapshotId, Instant createdAt) {
		this.id = id;
		this.snapshotId = snapshotId;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getSnapshotId() {
		return snapshotId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public int getOverallScore() {
		return overallScore;
	}

	public void setOverallScore(int overallScore) {
		this.overallScore = overallScore;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public LinkedInAnalysisReport getReport() {
		return report;
	}

	public void setReport(LinkedInAnalysisReport report) {
		this.report = report;
	}

	public boolean isImagesAnalyzed() {
		return imagesAnalyzed;
	}

	public void setImagesAnalyzed(boolean imagesAnalyzed) {
		this.imagesAnalyzed = imagesAnalyzed;
	}

	public String getAiModel() {
		return aiModel;
	}

	public void setAiModel(String aiModel) {
		this.aiModel = aiModel;
	}

	public Integer getTokensIn() {
		return tokensIn;
	}

	public void setTokensIn(Integer tokensIn) {
		this.tokensIn = tokensIn;
	}

	public Integer getTokensOut() {
		return tokensOut;
	}

	public void setTokensOut(Integer tokensOut) {
		this.tokensOut = tokensOut;
	}

	public BigDecimal getCostUsd() {
		return costUsd;
	}

	public void setCostUsd(BigDecimal costUsd) {
		this.costUsd = costUsd;
	}
}
