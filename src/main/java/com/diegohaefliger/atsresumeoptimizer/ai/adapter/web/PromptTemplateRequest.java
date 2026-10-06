package com.diegohaefliger.atsresumeoptimizer.ai.adapter.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record PromptTemplateRequest(@NotBlank @Size(max = 100_000) String content) {
}
