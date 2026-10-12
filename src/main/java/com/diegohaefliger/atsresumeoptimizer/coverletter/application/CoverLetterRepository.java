package com.diegohaefliger.atsresumeoptimizer.coverletter.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CoverLetterRepository extends JpaRepository<CoverLetterEntity, UUID> {

	List<CoverLetterEntity> findByJobPostingIdOrderByCreatedAtDesc(UUID jobPostingId);

	Optional<CoverLetterEntity> findByIdAndJobPostingId(UUID id, UUID jobPostingId);
}
