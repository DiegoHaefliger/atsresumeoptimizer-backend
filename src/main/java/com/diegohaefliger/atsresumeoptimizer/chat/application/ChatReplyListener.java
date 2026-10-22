package com.diegohaefliger.atsresumeoptimizer.chat.application;

import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatMessage;

public interface ChatReplyListener {

	/** Devolver {@code false} (quem ouvia foi embora) interrompe a resposta; o que já saiu fica salvo. */
	boolean onToken(String token);

	void onDone(ChatMessage assistantMessage);

	void onError(String message);
}
