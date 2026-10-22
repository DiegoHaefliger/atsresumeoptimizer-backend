package com.diegohaefliger.atsresumeoptimizer.chat.application;

import com.diegohaefliger.atsresumeoptimizer.ai.AiCallException;
import com.diegohaefliger.atsresumeoptimizer.ai.AiUsage;
import com.diegohaefliger.atsresumeoptimizer.ai.ChatBrief;
import com.diegohaefliger.atsresumeoptimizer.ai.ChatPort;
import com.diegohaefliger.atsresumeoptimizer.ai.ChatReply;
import com.diegohaefliger.atsresumeoptimizer.ai.ChatTurn;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatMessage;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatRole;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
class ChatReplyServiceImpl implements ChatReplyService {

	static final int HISTORY_MESSAGE_LIMIT = 40;
	static final int HISTORY_CHAR_BUDGET = 48_000;
	static final String EMPTY_REPLY = "A IA não devolveu resposta. Tente de novo.";

	private static final Logger LOGGER = LoggerFactory.getLogger(ChatReplyServiceImpl.class);

	private final ChatMessageStore store;
	private final ChatPort chatPort;

	ChatReplyServiceImpl(ChatMessageStore store, ChatPort chatPort) {
		this.store = store;
		this.chatPort = chatPort;
	}

	@Override
	public PendingReply send(UUID conversationId, String content) {
		ChatMessage userMessage = store.addUserMessage(conversationId, content);
		ChatBrief brief = new ChatBrief("", withinBudget(store.recent(conversationId, HISTORY_MESSAGE_LIMIT)));
		return new Pending(conversationId, userMessage, brief);
	}

	private static List<ChatTurn> withinBudget(List<ChatMessage> history) {
		Deque<ChatTurn> kept = new ArrayDeque<>();
		int used = 0;
		for (int index = history.size() - 1; index >= 0; index--) {
			ChatMessage message = history.get(index);
			used += message.content().length();
			if (used > HISTORY_CHAR_BUDGET && !kept.isEmpty()) {
				break;
			}
			kept.addFirst(new ChatTurn(
					message.role() == ChatRole.USER ? ChatTurn.Speaker.USER : ChatTurn.Speaker.ASSISTANT, message.content()));
		}
		return List.copyOf(kept);
	}

	private final class Pending implements PendingReply {

		private final UUID conversationId;
		private final ChatMessage userMessage;
		private final ChatBrief brief;

		private Pending(UUID conversationId, ChatMessage userMessage, ChatBrief brief) {
			this.conversationId = conversationId;
			this.userMessage = userMessage;
			this.brief = brief;
		}

		@Override
		public ChatMessage userMessage() {
			return userMessage;
		}

		@Override
		public void run(ChatReplyListener listener) {
			StringBuilder streamed = new StringBuilder();
			try {
				ChatReply reply = chatPort.stream(brief, token -> {
					streamed.append(token);
					return listener.onToken(token);
				});
				if (reply.text().isBlank()) {
					if (!reply.interrupted()) {
						listener.onError(EMPTY_REPLY);
					}
					return;
				}
				listener.onDone(store.addAssistantMessage(conversationId, reply.text(), reply.usage(), reply.interrupted()));
			} catch (AiCallException failure) {
				LOGGER.warn("Falha na resposta do chat da conversa {}", conversationId, failure);
				if (!streamed.isEmpty()) {
					store.addAssistantMessage(conversationId, streamed.toString(), AiUsage.NONE, true);
				}
				listener.onError(failure.getMessage());
			}
		}
	}
}
