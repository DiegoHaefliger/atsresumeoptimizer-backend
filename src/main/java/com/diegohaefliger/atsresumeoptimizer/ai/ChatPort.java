package com.diegohaefliger.atsresumeoptimizer.ai;

public interface ChatPort {

	/** Bloqueia até o fim da resposta ou até o listener mandar parar; só cai pro próximo provedor se nada saiu ainda. */
	ChatReply stream(ChatBrief brief, ChatStreamListener listener);
}
