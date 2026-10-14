package com.diegohaefliger.atsresumeoptimizer.jobnote.application;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface JobNoteRepository extends JpaRepository<JobNoteEntity, UUID> {

	Optional<JobNoteEntity> findByJobPostingId(UUID jobPostingId);
}
