package com.diegohaefliger.atsresumeoptimizer.ai;

@FunctionalInterface
public interface ChatStreamListener {

	/** Devolver {@code false} interrompe a resposta. */
	boolean onToken(String token);
}
