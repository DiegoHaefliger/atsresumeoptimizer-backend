package com.diegohaefliger.atsresumeoptimizer.resume.application;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ResumeVersionRepository extends JpaRepository<ResumeVersion, UUID> {

	List<ResumeVersion> findByResumeId(UUID resumeId);

	List<ResumeVersion> findByResumeIdOrderByCreatedAtAsc(UUID resumeId);

	List<ResumeVersion> findByResumeIdInOrderByCreatedAtAsc(Collection<UUID> resumeIds);

	List<ResumeVersion> findByCreatedAtBeforeAndStorageKeyNot(Instant cutoff, String scrubbedPlaceholder);
}
