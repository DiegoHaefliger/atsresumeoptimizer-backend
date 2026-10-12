package com.diegohaefliger.atsresumeoptimizer.interview.application;

import com.github.f4b6a3.uuid.UuidCreator;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "interview_question_set")
class InterviewQuestionSetEntity {

	@Id
	private UUID id;

	@Column(name = "job_posting_id")
	private UUID jobPostingId;

	@Column(name = "resume_version_id")
	private UUID resumeVersionId;

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

	@ElementCollection
	@CollectionTable(name = "interview_question", joinColumns = @JoinColumn(name = "set_id"))
	@OrderColumn(name = "position")
	private List<InterviewQuestionValue> questions = new ArrayList<>();

	protected InterviewQuestionSetEntity() {
	}

	InterviewQuestionSetEntity(UUID jobPostingId, UUID resumeVersionId, Instant createdAt) {
		this.id = UuidCreator.getTimeOrderedEpoch();
		this.jobPostingId = jobPostingId;
		this.resumeVersionId = resumeVersionId;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getJobPostingId() {
		return jobPostingId;
	}

	public UUID getResumeVersionId() {
		return resumeVersionId;
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

	public List<InterviewQuestionValue> getQuestions() {
		return questions;
	}

	public void setQuestions(List<InterviewQuestionValue> questions) {
		this.questions = new ArrayList<>(questions);
	}
}
