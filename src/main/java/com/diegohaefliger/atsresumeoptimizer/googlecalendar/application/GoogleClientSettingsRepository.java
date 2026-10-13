package com.diegohaefliger.atsresumeoptimizer.googlecalendar.application;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface GoogleClientSettingsRepository extends JpaRepository<GoogleClientSettingsEntity, UUID> {

	Optional<GoogleClientSettingsEntity> findFirstByOrderByUpdatedAtDesc();
}
