package com.diegohaefliger.atsresumeoptimizer.interview.application;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
class InterviewQuestionValue {

	private String question;

	private String requirement;

	private String reason;

	@Column(name = "answer_tip")
	private String answerTip;

	protected InterviewQuestionValue() {
	}

	InterviewQuestionValue(String question, String requirement, String reason, String answerTip) {
		this.question = question;
		this.requirement = requirement;
		this.reason = reason;
		this.answerTip = answerTip;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public String getRequirement() {
		return requirement;
	}

	public void setRequirement(String requirement) {
		this.requirement = requirement;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getAnswerTip() {
		return answerTip;
	}

	public void setAnswerTip(String answerTip) {
		this.answerTip = answerTip;
	}
}
