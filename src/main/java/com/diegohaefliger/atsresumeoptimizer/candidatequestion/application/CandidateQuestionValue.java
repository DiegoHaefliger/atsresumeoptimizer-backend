package com.diegohaefliger.atsresumeoptimizer.candidatequestion.application;

import jakarta.persistence.Embeddable;

@Embeddable
class CandidateQuestionValue {

	private String question;

	private String topic;

	private String reason;

	protected CandidateQuestionValue() {
	}

	CandidateQuestionValue(String question, String topic, String reason) {
		this.question = question;
		this.topic = topic;
		this.reason = reason;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}
}
