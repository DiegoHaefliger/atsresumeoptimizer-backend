package com.diegohaefliger.atsresumeoptimizer.ai.application;

import java.time.Instant;

public record PromptTemplateView(String key, int version, String model, String content, Instant createdAt) {
}
