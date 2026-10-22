package com.diegohaefliger.atsresumeoptimizer.chat.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatConversationDetail(
		UUID id,
		String title,
		Instant createdAt,
		Instant updatedAt,
		List<ChatAttachment> attachments,
		List<ChatMessage> messages) {

	public ChatConversationDetail {
		attachments = List.copyOf(attachments);
		messages = List.copyOf(messages);
	}
}
