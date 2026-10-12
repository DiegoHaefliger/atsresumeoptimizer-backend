package com.diegohaefliger.atsresumeoptimizer.candidatequestion.application;

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
@Table(name = "candidate_question_set")
class CandidateQuestionSetEntity {

	@Id
	private UUID id;

	@Column(name = "job_posting_id")
	private UUID jobPostingId;

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
	@CollectionTable(name = "candidate_question", joinColumns = @JoinColumn(name = "set_id"))
	@OrderColumn(name = "position")
	private List<CandidateQuestionValue> questions = new ArrayList<>();

	protected CandidateQuestionSetEntity() {
	}

	CandidateQuestionSetEntity(UUID jobPostingId, Instant createdAt) {
		this.id = UuidCreator.getTimeOrderedEpoch();
		this.jobPostingId = jobPostingId;
		this.createdAt = createdAt;
	}

	public UUID getId() {
		return id;
	}

	public UUID getJobPostingId() {
		return jobPostingId;
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

	public List<CandidateQuestionValue> getQuestions() {
		return questions;
	}

	public void setQuestions(List<CandidateQuestionValue> questions) {
		this.questions = new ArrayList<>(questions);
	}
}
