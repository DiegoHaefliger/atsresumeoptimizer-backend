package com.diegohaefliger.atsresumeoptimizer.googlecalendar.application;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface GoogleAccountRepository extends JpaRepository<GoogleAccountEntity, UUID> {

	Optional<GoogleAccountEntity> findFirstByOrderByUpdatedAtDesc();
}
