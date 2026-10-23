package com.diegohaefliger.atsresumeoptimizer.coverletter.application;

import com.diegohaefliger.atsresumeoptimizer.ai.ModelUsage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CoverLetterRepository extends JpaRepository<CoverLetterEntity, UUID> {

	List<CoverLetterEntity> findByJobPostingIdOrderByCreatedAtDesc(UUID jobPostingId);

	Optional<CoverLetterEntity> findByIdAndJobPostingId(UUID id, UUID jobPostingId);

	@Query("""
			select new com.diegohaefliger.atsresumeoptimizer.ai.ModelUsage(
			       c.aiModel, coalesce(sum(c.tokensIn), 0L), coalesce(sum(c.tokensOut), 0L),
			       coalesce(sum(c.costUsd), 0bd), count(c),
			       coalesce(sum(case when coalesce(c.tokensIn, 0) + coalesce(c.tokensOut, 0) = 0 then 1L else 0L end), 0L))
			from CoverLetterEntity c
			where c.jobPostingId = :jobPostingId
			group by c.aiModel
			""")
	List<ModelUsage> usageByModelForJobPostingId(@Param("jobPostingId") UUID jobPostingId);
}
