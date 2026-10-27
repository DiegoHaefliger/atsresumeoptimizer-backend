package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PairingTokenRepository extends JpaRepository<PairingTokenEntity, UUID> {

	Optional<PairingTokenEntity> findFirstByOrderByCreatedAtDesc();
}
