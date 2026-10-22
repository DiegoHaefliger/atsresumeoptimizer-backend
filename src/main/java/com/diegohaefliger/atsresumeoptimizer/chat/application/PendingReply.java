package com.diegohaefliger.atsresumeoptimizer.chat.application;

import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatMessage;

public interface PendingReply {

	ChatMessage userMessage();

	/** Bloqueia até a resposta terminar; sem transação aberta durante a chamada à IA. */
	void run(ChatReplyListener listener);
}
