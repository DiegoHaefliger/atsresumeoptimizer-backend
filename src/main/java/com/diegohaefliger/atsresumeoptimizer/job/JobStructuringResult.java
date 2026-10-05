package com.diegohaefliger.atsresumeoptimizer.job;

import com.diegohaefliger.atsresumeoptimizer.ai.AiUsage;
import com.diegohaefliger.atsresumeoptimizer.ai.JobStructured;
import java.util.UUID;

public record JobStructuringResult(UUID jobPostingId, JobStructured structured, AiUsage usage) {
}
