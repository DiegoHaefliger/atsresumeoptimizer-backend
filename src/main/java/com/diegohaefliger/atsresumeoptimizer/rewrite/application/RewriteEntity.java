package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "resume_rewrite")
class RewriteEntity {

	@Id
	private UUID id;

	@Column(name = "analysis_id")
	private UUID analysisId;

	@Column(name = "docx_resume_version_id")
	private UUID docxResumeVersionId;

	@Column(name = "pdf_resume_version_id")
	private UUID pdfResumeVersionId;

	@Column(name = "ai_model")
	private String aiModel;

	@Column(name = "tokens_in")
	private Integer tokensIn;

	@Column(name = "tokens_out")
	private Integer tokensOut;

	@Column(name = "cost_usd")
	private BigDecimal costUsd;

	@Column(name = "job_highlighted")
	private boolean jobHighlighted;

	@Column(name = "created_at")
	private Instant createdAt;

	protected RewriteEntity() {
	}

	RewriteEntity(
			UUID id,
			UUID analysisId,
			UUID docxResumeVersionId,
			UUID pdfResumeVersionId,
			String aiModel,
			int tokensIn,
			int tokensOut,
			BigDecimal costUsd,
			boolean jobHighlighted) {
		this.id = id;
		this.analysisId = analysisId;
		this.docxResumeVersionId = docxResumeVersionId;
		this.pdfResumeVersionId = pdfResumeVersionId;
		this.aiModel = aiModel;
		this.tokensIn = tokensIn;
		this.tokensOut = tokensOut;
		this.costUsd = costUsd;
		this.jobHighlighted = jobHighlighted;
		this.createdAt = Instant.now();
	}

	String aiModel() {
		return aiModel;
	}

	boolean jobHighlighted() {
		return jobHighlighted;
	}
}
