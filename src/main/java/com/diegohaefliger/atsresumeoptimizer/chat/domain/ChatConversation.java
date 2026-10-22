package com.diegohaefliger.atsresumeoptimizer.chat.domain;

import java.time.Instant;
import java.util.UUID;

public record ChatConversation(UUID id, String title, Instant createdAt, Instant updatedAt) {
}
