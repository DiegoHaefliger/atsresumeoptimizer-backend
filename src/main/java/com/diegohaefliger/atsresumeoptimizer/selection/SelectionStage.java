package com.diegohaefliger.atsresumeoptimizer.selection;

public enum SelectionStage {
	INTERESTED("Interessado"),
	APPLIED("Candidatado"),
	SCREENING("Triagem"),
	TECHNICAL_TEST("Teste técnico"),
	TECHNICAL_INTERVIEW("Entrevista técnica"),
	MANAGER_INTERVIEW("Entrevista com gestor"),
	OFFER("Proposta"),
	HIRED("Contratado"),
	REJECTED("Reprovado"),
	WITHDRAWN("Desistência");

	private final String label;

	SelectionStage(String label) {
		this.label = label;
	}

	public String label() {
		return label;
	}
}
