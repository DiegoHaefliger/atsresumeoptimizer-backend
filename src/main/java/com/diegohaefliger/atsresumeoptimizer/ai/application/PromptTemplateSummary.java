package com.diegohaefliger.atsresumeoptimizer.ai.application;

import java.time.Instant;

public record PromptTemplateSummary(String key, int version, String model, Instant createdAt) {
}
