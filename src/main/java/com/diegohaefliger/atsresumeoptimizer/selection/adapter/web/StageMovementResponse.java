package com.diegohaefliger.atsresumeoptimizer.selection.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import java.time.Instant;

record StageMovementResponse(SelectionStage stage, String note, Instant movedAt) {
}
