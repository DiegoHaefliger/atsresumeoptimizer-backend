package com.diegohaefliger.atsresumeoptimizer.language.domain;

public class GrammarCheckException extends RuntimeException {

	public GrammarCheckException(Throwable cause) {
		super("Falha ao verificar ortografia/gramática do texto.", cause);
	}
}
