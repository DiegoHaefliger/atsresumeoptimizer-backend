package com.diegohaefliger.atsresumeoptimizer.chat.application;

import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatFile;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatMessage;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
class ChatMessageAssembler {

	private final ChatFileRepository files;
	private final ChatEntityMapper mapper;

	ChatMessageAssembler(ChatFileRepository files, ChatEntityMapper mapper) {
		this.files = files;
		this.mapper = mapper;
	}

	List<ChatMessage> assemble(List<ChatMessageEntity> messages) {
		if (messages.isEmpty()) {
			return List.of();
		}
		Map<UUID, List<ChatFile>> byMessage = files
				.findByMessageIdInOrderByIdAsc(messages.stream().map(ChatMessageEntity::getId).toList()).stream()
				.collect(Collectors.groupingBy(ChatFileEntity::getMessageId,
						Collectors.mapping(mapper::toDomain, Collectors.toList())));
		return messages.stream()
				.map(message -> attach(mapper.toDomain(message), byMessage.getOrDefault(message.getId(), List.of())))
				.toList();
	}

	static ChatMessage attach(ChatMessage message, List<ChatFile> files) {
		return new ChatMessage(message.id(), message.role(), message.content(), message.aiModel(), message.tokensIn(),
				message.tokensOut(), message.costUsd(), message.interrupted(), message.createdAt(), files);
	}

	ChatMessage assemble(ChatMessageEntity message) {
		return assemble(List.of(message)).getFirst();
	}
}
