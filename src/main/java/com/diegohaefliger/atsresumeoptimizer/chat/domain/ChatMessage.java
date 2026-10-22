package com.diegohaefliger.atsresumeoptimizer.chat.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatMessage(
		UUID id,
		ChatRole role,
		String content,
		String aiModel,
		int tokensIn,
		int tokensOut,
		BigDecimal costUsd,
		boolean interrupted,
		Instant createdAt,
		List<ChatFile> files) {

	public ChatMessage {
		files = files == null ? List.of() : List.copyOf(files);
	}
}
