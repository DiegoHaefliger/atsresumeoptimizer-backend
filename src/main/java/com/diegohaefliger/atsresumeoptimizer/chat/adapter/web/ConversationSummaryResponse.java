package com.diegohaefliger.atsresumeoptimizer.chat.adapter.web;

import java.time.Instant;
import java.util.UUID;

record ConversationSummaryResponse(UUID id, String title, Instant createdAt, Instant updatedAt) {
}
