package com.diegohaefliger.atsresumeoptimizer.preference.application;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface JobPreferenceRepository extends JpaRepository<JobPreferenceEntity, UUID> {

	Optional<JobPreferenceEntity> findFirstByOrderByUpdatedAtDesc();
}
