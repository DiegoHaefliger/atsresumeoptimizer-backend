package com.diegohaefliger.atsresumeoptimizer.chat.application;

import com.diegohaefliger.atsresumeoptimizer.ai.AiUsage;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatMessage;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatRole;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.NothingToRegenerateException;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.UnknownConversationException;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class ChatMessageStore {

	private static final int TITLE_MAX_LENGTH = 60;

	private final ChatConversationRepository conversations;
	private final ChatMessageRepository messages;
	private final ChatFileRepository files;
	private final ChatMessageAssembler assembler;
	private final ChatEntityMapper mapper;
	private final Clock clock;

	ChatMessageStore(ChatConversationRepository conversations, ChatMessageRepository messages, ChatFileRepository files,
			ChatMessageAssembler assembler, ChatEntityMapper mapper, Clock clock) {
		this.conversations = conversations;
		this.messages = messages;
		this.files = files;
		this.assembler = assembler;
		this.mapper = mapper;
		this.clock = clock;
	}

	@Transactional
	ChatMessage addUserMessage(UUID conversationId, String content, List<PreparedChatFile> attachments) {
		ChatConversationEntity conversation = conversations.findById(conversationId)
				.orElseThrow(() -> new UnknownConversationException(conversationId));
		Instant now = clock.instant();
		if (ChatConversationServiceImpl.DEFAULT_TITLE.equals(conversation.getTitle())
				&& !messages.existsByConversationId(conversationId)) {
			conversation.rename(titleFrom(content.isBlank() ? attachments.getFirst().fileName() : content), now);
		} else {
			conversation.touch(now);
		}
		ChatMessageEntity message = messages.save(new ChatMessageEntity(conversationId, ChatRole.USER, content, now));
		List<ChatFileEntity> saved = files.saveAll(attachments.stream().map(file -> new ChatFileEntity(message.getId(), file)).toList());
		return ChatMessageAssembler.attach(mapper.toDomain(message), mapper.toFiles(saved));
	}

	@Transactional
	ChatMessage addAssistantMessage(UUID conversationId, String content, AiUsage usage, boolean interrupted) {
		Instant now = clock.instant();
		ChatMessageEntity message = new ChatMessageEntity(conversationId, ChatRole.ASSISTANT, content, now);
		message.recordAiResult(usage.model(), usage.tokensIn(), usage.tokensOut(), usage.costUsd(), interrupted);
		conversations.findById(conversationId).ifPresent(conversation -> conversation.touch(now));
		return mapper.toDomain(messages.save(message));
	}

	@Transactional
	ChatMessage reopenLastQuestion(UUID conversationId) {
		List<ChatMessageEntity> newestFirst = messages.findByConversationIdOrderByCreatedAtDesc(conversationId, Limit.of(2));
		if (!newestFirst.isEmpty() && newestFirst.getFirst().getRole() == ChatRole.ASSISTANT) {
			messages.delete(newestFirst.removeFirst());
		}
		if (newestFirst.isEmpty() || newestFirst.getFirst().getRole() != ChatRole.USER) {
			throw new NothingToRegenerateException();
		}
		conversations.findById(conversationId).ifPresent(conversation -> conversation.touch(clock.instant()));
		return assembler.assemble(newestFirst.getFirst());
	}

	@Transactional(readOnly = true)
	List<ChatMessage> recent(UUID conversationId, int limit) {
		List<ChatMessageEntity> newestFirst = new ArrayList<>(
				messages.findByConversationIdOrderByCreatedAtDesc(conversationId, Limit.of(limit)));
		Collections.reverse(newestFirst);
		return assembler.assemble(newestFirst);
	}

	@Transactional(readOnly = true)
	Map<UUID, List<ChatFileEntity>> filesOf(Collection<UUID> messageIds) {
		if (messageIds.isEmpty()) {
			return Map.of();
		}
		return files.findByMessageIdInOrderByIdAsc(messageIds).stream()
				.collect(Collectors.groupingBy(ChatFileEntity::getMessageId));
	}

	private static String titleFrom(String content) {
		String firstLine = content.strip().lines().findFirst().orElse("").strip();
		return firstLine.length() <= TITLE_MAX_LENGTH ? firstLine : firstLine.substring(0, TITLE_MAX_LENGTH - 1).stripTrailing() + "…";
	}
}
