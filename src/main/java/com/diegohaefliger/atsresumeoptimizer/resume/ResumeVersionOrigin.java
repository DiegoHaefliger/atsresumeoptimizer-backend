package com.diegohaefliger.atsresumeoptimizer.resume;

import java.util.Optional;
import java.util.UUID;

public record ResumeVersionOrigin(int number, Optional<UUID> sourceAnalysisId) {
}
