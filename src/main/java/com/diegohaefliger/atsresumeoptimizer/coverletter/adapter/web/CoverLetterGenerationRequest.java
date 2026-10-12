package com.diegohaefliger.atsresumeoptimizer.coverletter.adapter.web;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

record CoverLetterGenerationRequest(@NotNull UUID resumeVersionId) {
}
