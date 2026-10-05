package com.diegohaefliger.atsresumeoptimizer.ai.application;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface AiProviderConfigRepository extends JpaRepository<AiProviderConfigEntity, AiProvider> {

	List<AiProviderConfigEntity> findByEnabledTrueOrderByPriorityAsc();
}
