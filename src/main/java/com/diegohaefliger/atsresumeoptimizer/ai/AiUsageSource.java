package com.diegohaefliger.atsresumeoptimizer.ai;

import java.util.List;
import java.util.UUID;

/** Cada módulo que grava consumo de IA expõe o seu por aqui, sem abrir a entidade. */
public interface AiUsageSource {

	AiStage stage();

	List<ModelUsage> usageByModel(UUID jobPostingId);
}
