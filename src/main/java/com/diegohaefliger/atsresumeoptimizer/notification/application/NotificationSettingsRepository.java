package com.diegohaefliger.atsresumeoptimizer.notification.application;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface NotificationSettingsRepository extends JpaRepository<NotificationSettingsEntity, UUID> {

	Optional<NotificationSettingsEntity> findFirstByOrderByUpdatedAtDesc();
}
