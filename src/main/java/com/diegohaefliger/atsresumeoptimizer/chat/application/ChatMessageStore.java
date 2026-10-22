package com.diegohaefliger.atsresumeoptimizer.chat.application;

import com.diegohaefliger.atsresumeoptimizer.ai.AiUsage;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatMessage;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatRole;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.UnknownConversationException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ChatMessageStore {

	private static final int TITLE_MAX_LENGTH = 60;

	private final ChatConversationRepository conversations;
	private final ChatMessageRepository messages;
	private final ChatEntityMapper mapper;
	private final Clock clock;

	ChatMessageStore(ChatConversationRepository conversations, ChatMessageRepository messages, ChatEntityMapper mapper,
			Clock clock) {
		this.conversations = conversations;
		this.messages = messages;
		this.mapper = mapper;
		this.clock = clock;
	}

	@Transactional
	ChatMessage addUserMessage(UUID conversationId, String content) {
		ChatConversationEntity conversation = conversations.findById(conversationId)
				.orElseThrow(() -> new UnknownConversationException(conversationId));
		Instant now = clock.instant();
		if (ChatConversationServiceImpl.DEFAULT_TITLE.equals(conversation.getTitle())
				&& !messages.existsByConversationId(conversationId)) {
			conversation.rename(titleFrom(content), now);
		} else {
			conversation.touch(now);
		}
		return mapper.toDomain(messages.save(new ChatMessageEntity(conversationId, ChatRole.USER, content, now)));
	}

	@Transactional
	ChatMessage addAssistantMessage(UUID conversationId, String content, AiUsage usage, boolean interrupted) {
		Instant now = clock.instant();
		ChatMessageEntity message = new ChatMessageEntity(conversationId, ChatRole.ASSISTANT, content, now);
		message.recordAiResult(usage.model(), usage.tokensIn(), usage.tokensOut(), usage.costUsd(), interrupted);
		conversations.findById(conversationId).ifPresent(conversation -> conversation.touch(now));
		return mapper.toDomain(messages.save(message));
	}

	@Transactional(readOnly = true)
	List<ChatMessage> recent(UUID conversationId, int limit) {
		List<ChatMessage> newestFirst = new ArrayList<>(
				mapper.toMessages(messages.findByConversationIdOrderByCreatedAtDesc(conversationId, Limit.of(limit))));
		Collections.reverse(newestFirst);
		return newestFirst;
	}

	private static String titleFrom(String content) {
		String firstLine = content.strip().lines().findFirst().orElse("").strip();
		return firstLine.length() <= TITLE_MAX_LENGTH ? firstLine : firstLine.substring(0, TITLE_MAX_LENGTH - 1).stripTrailing() + "…";
	}
}
