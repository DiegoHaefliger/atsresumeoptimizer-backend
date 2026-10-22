package com.diegohaefliger.atsresumeoptimizer.ai;

public interface ChatStreamListener {

	/** Devolver {@code false} interrompe a resposta. */
	boolean onToken(String token);

	default void onToolUse(String label) {
	}

	/** O modelo recusou imagens; a resposta segue só com o texto. */
	default void onImagesUnavailable() {
	}

	/** O provedor/modelo recusou ferramentas; a resposta segue só com os dados já anexados. */
	default void onToolsUnavailable() {
	}
}
