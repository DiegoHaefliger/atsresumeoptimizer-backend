package com.diegohaefliger.atsresumeoptimizer.ai;

/** Não é {@code BusinessException}: não é erro do cliente da API, quem chama decide entre {@code PARTIAL} e {@code FAILED}. */
public class AiCallException extends RuntimeException {

	public AiCallException(String message, Throwable cause) {
		super(message, cause);
	}
}
