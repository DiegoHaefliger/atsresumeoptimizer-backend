package com.diegohaefliger.atsresumeoptimizer.chat.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

record MessageResponse(
		UUID id,
		ChatRole role,
		String content,
		String aiModel,
		int tokensIn,
		int tokensOut,
		BigDecimal costUsd,
		boolean interrupted,
		Instant createdAt) {
}
