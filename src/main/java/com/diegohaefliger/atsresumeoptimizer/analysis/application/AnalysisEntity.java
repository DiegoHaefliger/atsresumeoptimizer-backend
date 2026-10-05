package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.ai.AiUsage;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisStatus;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.AnalysisMode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "analysis")
class AnalysisEntity {

	@Id
	private UUID id;

	@Column(name = "resume_version_id")
	private UUID resumeVersionId;

	@Column(name = "job_posting_id")
	private UUID jobPostingId;

	@Enumerated(EnumType.STRING)
	private AnalysisMode mode;

	@Column(name = "target_role")
	private String targetRole;

	@Column(name = "job_description")
	private String jobDescription;

	@Enumerated(EnumType.STRING)
	private AnalysisStatus status;

	@Column(name = "overall_score")
	private Integer overallScore;

	@Column(name = "scoring_profile_id")
	private UUID scoringProfileId;

	@Column(name = "ai_model")
	private String aiModel;

	@Column(name = "tokens_in")
	private Integer tokensIn;

	@Column(name = "tokens_out")
	private Integer tokensOut;

	@Column(name = "cost_usd")
	private BigDecimal costUsd;

	private String error;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "finished_at")
	private Instant finishedAt;

	@Column(name = "created_at")
	private Instant createdAt;

	protected AnalysisEntity() {
	}

	AnalysisEntity(
			UUID id,
			UUID resumeVersionId,
			AnalysisMode mode,
			String targetRole,
			String jobDescription,
			Instant startedAt) {
		this.id = id;
		this.resumeVersionId = resumeVersionId;
		this.mode = mode;
		this.targetRole = targetRole;
		this.jobDescription = jobDescription;
		this.status = AnalysisStatus.PENDING;
		this.startedAt = startedAt;
		this.createdAt = Instant.now();
	}

	void linkJobPosting(UUID jobPostingId) {
		this.jobPostingId = jobPostingId;
	}

	void startParsing() {
		this.status = AnalysisStatus.PARSING;
	}

	void startAnalyzing() {
		this.status = AnalysisStatus.ANALYZING;
	}

	void complete(
			UUID jobPostingId,
			AnalysisStatus status,
			Integer overallScore,
			UUID scoringProfileId,
			AiUsage usage,
			String error) {
		this.jobPostingId = jobPostingId;
		this.status = status;
		this.overallScore = overallScore;
		this.scoringProfileId = scoringProfileId;
		this.aiModel = usage.model();
		this.tokensIn = usage.tokensIn();
		this.tokensOut = usage.tokensOut();
		this.costUsd = usage.costUsd();
		this.error = error;
		this.finishedAt = Instant.now();
	}

	UUID id() {
		return id;
	}

	UUID resumeVersionId() {
		return resumeVersionId;
	}

	AnalysisMode mode() {
		return mode;
	}

	UUID jobPostingId() {
		return jobPostingId;
	}

	String targetRole() {
		return targetRole;
	}

	String jobDescription() {
		return jobDescription;
	}

	AnalysisStatus status() {
		return status;
	}

	Integer overallScore() {
		return overallScore;
	}

	String error() {
		return error;
	}
}
