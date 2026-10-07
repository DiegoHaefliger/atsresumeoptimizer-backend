package com.diegohaefliger.atsresumeoptimizer.coverletter.application;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CoverLetterRepository extends JpaRepository<CoverLetterEntity, UUID> {

	Optional<CoverLetterEntity> findByJobPostingId(UUID jobPostingId);
}
