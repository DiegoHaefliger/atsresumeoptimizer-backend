package com.diegohaefliger.atsresumeoptimizer.selection.domain;

import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import java.time.Instant;

public record StageMovement(SelectionStage stage, String note, Instant movedAt) {
}
