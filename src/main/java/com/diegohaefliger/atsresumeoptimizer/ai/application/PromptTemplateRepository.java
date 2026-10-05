package com.diegohaefliger.atsresumeoptimizer.ai.application;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PromptTemplateRepository extends JpaRepository<PromptTemplate, UUID> {

	Optional<PromptTemplate> findFirstByKeyAndActiveTrueOrderByVersionDesc(String key);
}
