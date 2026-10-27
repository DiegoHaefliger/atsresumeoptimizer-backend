package com.diegohaefliger.atsresumeoptimizer.linkedin.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface LinkedInSnapshotRepository extends JpaRepository<LinkedInSnapshotEntity, UUID> {

	Optional<LinkedInSnapshotEntity> findFirstByOrderByCapturedAtDesc();

	List<LinkedInSnapshotHead> findAllByOrderByCapturedAtDesc();
}
