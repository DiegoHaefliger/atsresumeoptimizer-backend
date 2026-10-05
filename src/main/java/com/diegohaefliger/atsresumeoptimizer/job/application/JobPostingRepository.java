package com.diegohaefliger.atsresumeoptimizer.job.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface JobPostingRepository extends JpaRepository<JobPosting, UUID> {

	Optional<JobPosting> findByTextHash(String textHash);

	List<JobPosting> findTop100BySyntheticFalseAndHiddenAtIsNullOrderByCreatedAtDesc();
}
