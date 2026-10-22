package com.diegohaefliger.atsresumeoptimizer.chat.application;

import java.util.UUID;

public interface ChatReplyService {

	/** Grava a pergunta e prepara a resposta; falha aqui (conversa inexistente) acontece antes do stream abrir. */
	PendingReply send(UUID conversationId, String content);

	/** Descarta a última resposta (se houver) e responde de novo a última pergunta. */
	PendingReply regenerate(UUID conversationId);
}
