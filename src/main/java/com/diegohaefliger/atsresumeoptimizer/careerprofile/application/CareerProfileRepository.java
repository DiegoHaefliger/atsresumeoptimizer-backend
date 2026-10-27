package com.diegohaefliger.atsresumeoptimizer.careerprofile.application;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CareerProfileRepository extends JpaRepository<CareerProfileEntity, UUID> {

	Optional<CareerProfileEntity> findFirstByOrderByUpdatedAtDesc();
}
