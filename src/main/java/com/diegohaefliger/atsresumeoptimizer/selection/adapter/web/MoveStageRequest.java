package com.diegohaefliger.atsresumeoptimizer.selection.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record MoveStageRequest(@NotNull SelectionStage stage, @Size(max = 2000) String note) {
}
