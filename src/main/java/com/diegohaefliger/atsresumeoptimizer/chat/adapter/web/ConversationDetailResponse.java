package com.diegohaefliger.atsresumeoptimizer.chat.adapter.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

record ConversationDetailResponse(
		UUID id,
		String title,
		Instant createdAt,
		Instant updatedAt,
		List<AttachmentResponse> attachments,
		List<MessageResponse> messages) {
}
