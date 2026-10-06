package com.diegohaefliger.atsresumeoptimizer.selection.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import java.time.Instant;
import java.util.UUID;

record StageMovementResponse(UUID id, SelectionStage stage, String note, Instant movedAt) {
}
