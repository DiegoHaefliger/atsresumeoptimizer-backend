package com.diegohaefliger.atsresumeoptimizer.selection.domain;

import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import java.time.Instant;
import java.util.UUID;

public record StageMovement(UUID id, SelectionStage stage, String note, Instant movedAt) {
}
