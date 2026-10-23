package com.diegohaefliger.atsresumeoptimizer.job.application;

import com.diegohaefliger.atsresumeoptimizer.ai.ModelUsage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface JobPostingRepository extends JpaRepository<JobPosting, UUID> {

	Optional<JobPosting> findByTextHash(String textHash);

	List<JobPosting> findTop100BySyntheticFalseAndHiddenAtIsNullOrderByCreatedAtDesc();

	@Query("""
			select new com.diegohaefliger.atsresumeoptimizer.ai.ModelUsage(
			       p.aiModel, coalesce(p.tokensIn, 0), coalesce(p.tokensOut, 0), coalesce(p.costUsd, 0bd), 1L, 0L)
			from JobPosting p
			where p.id = :jobPostingId and p.aiModel is not null
			""")
	List<ModelUsage> usageByModelForJobPostingId(@Param("jobPostingId") UUID jobPostingId);
}
