package com.diegohaefliger.atsresumeoptimizer.chat.application;

import com.diegohaefliger.atsresumeoptimizer.ai.AiCallException;
import com.diegohaefliger.atsresumeoptimizer.ai.AiUsage;
import com.diegohaefliger.atsresumeoptimizer.ai.ChatBrief;
import com.diegohaefliger.atsresumeoptimizer.ai.ChatPort;
import com.diegohaefliger.atsresumeoptimizer.ai.ChatReply;
import com.diegohaefliger.atsresumeoptimizer.ai.ChatStreamListener;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatMessage;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.InvalidChatFileException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
class ChatReplyServiceImpl implements ChatReplyService {

	static final int HISTORY_MESSAGE_LIMIT = 40;
	static final String EMPTY_MESSAGE = "Escreva uma mensagem ou anexe um arquivo.";
	static final String EMPTY_REPLY = "A IA não devolveu resposta. Tente de novo.";

	private static final Logger LOGGER = LoggerFactory.getLogger(ChatReplyServiceImpl.class);

	private final ChatMessageStore store;
	private final ChatPort chatPort;
	private final ChatContextAssembler contextAssembler;
	private final ChatFileIntake fileIntake;

	ChatReplyServiceImpl(ChatMessageStore store, ChatPort chatPort, ChatContextAssembler contextAssembler,
			ChatFileIntake fileIntake) {
		this.store = store;
		this.chatPort = chatPort;
		this.contextAssembler = contextAssembler;
		this.fileIntake = fileIntake;
	}

	@Override
	public PendingReply send(UUID conversationId, String content, List<ChatUpload> uploads) {
		List<PreparedChatFile> attachments = fileIntake.prepare(uploads);
		if (content.isBlank() && attachments.isEmpty()) {
			throw new InvalidChatFileException(EMPTY_MESSAGE, HttpStatus.BAD_REQUEST);
		}
		return pendingFor(conversationId, store.addUserMessage(conversationId, content, attachments));
	}

	@Override
	public PendingReply regenerate(UUID conversationId) {
		return pendingFor(conversationId, store.reopenLastQuestion(conversationId));
	}

	private PendingReply pendingFor(UUID conversationId, ChatMessage userMessage) {
		List<ChatMessage> recent = store.recent(conversationId, HISTORY_MESSAGE_LIMIT);
		Map<UUID, List<ChatFileEntity>> files = store.filesOf(recent.stream().filter(ChatHistoryBuilder::hasFiles).map(ChatMessage::id).toList());
		ChatBrief brief = new ChatBrief(contextAssembler.contextFor(conversationId), ChatHistoryBuilder.build(recent, files),
				contextAssembler.tools());
		return new Pending(conversationId, userMessage, brief);
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
				ChatReply reply = chatPort.stream(brief, new ChatStreamListener() {
					@Override
					public boolean onToken(String token) {
						streamed.append(token);
						return listener.onToken(token);
					}

					@Override
					public void onToolUse(String label) {
						listener.onToolUse(label);
					}

					@Override
					public void onToolsUnavailable() {
						listener.onToolsUnavailable();
					}

					@Override
					public void onImagesUnavailable() {
						listener.onImagesUnavailable();
					}
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
