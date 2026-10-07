package com.diegohaefliger.atsresumeoptimizer.coverletter.application;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "cover_letter")
class CoverLetterEntity {

	@Id
	private UUID id;

	@Column(name = "job_posting_id")
	private UUID jobPostingId;

	@Column(name = "analysis_id")
	private UUID analysisId;

	private String content;

	@Column(name = "ai_model")
	private String aiModel;

	@Column(name = "tokens_in")
	private Integer tokensIn;

	@Column(name = "tokens_out")
	private Integer tokensOut;

	@Column(name = "cost_usd")
	private BigDecimal costUsd;

	@Column(name = "created_at")
	private Instant createdAt;

	@Column(name = "updated_at")
	private Instant updatedAt;

	protected CoverLetterEntity() {
	}

	CoverLetterEntity(UUID jobPostingId, Instant now) {
		this.id = UuidCreator.getTimeOrderedEpoch();
		this.jobPostingId = jobPostingId;
		this.createdAt = now;
		this.updatedAt = now;
	}

	public UUID getId() {
		return id;
	}

	public UUID getJobPostingId() {
		return jobPostingId;
	}

	public UUID getAnalysisId() {
		return analysisId;
	}

	public void setAnalysisId(UUID analysisId) {
		this.analysisId = analysisId;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
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

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
