package com.diegohaefliger.atsresumeoptimizer.ai.application;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface AiSettingsRepository extends JpaRepository<AiSettingsEntity, UUID> {

	Optional<AiSettingsEntity> findFirstByOrderByUpdatedAtDesc();
}
