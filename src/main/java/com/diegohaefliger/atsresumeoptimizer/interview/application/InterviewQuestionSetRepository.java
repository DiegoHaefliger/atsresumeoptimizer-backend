package com.diegohaefliger.atsresumeoptimizer.interview.application;

import com.diegohaefliger.atsresumeoptimizer.ai.ModelUsage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface InterviewQuestionSetRepository extends JpaRepository<InterviewQuestionSetEntity, UUID> {

	@EntityGraph(attributePaths = "questions")
	List<InterviewQuestionSetEntity> findByJobPostingIdOrderByCreatedAtDesc(UUID jobPostingId);

	@Query("""
			select new com.diegohaefliger.atsresumeoptimizer.ai.ModelUsage(
			       i.aiModel, coalesce(sum(i.tokensIn), 0L), coalesce(sum(i.tokensOut), 0L),
			       coalesce(sum(i.costUsd), 0bd), count(i),
			       coalesce(sum(case when coalesce(i.tokensIn, 0) + coalesce(i.tokensOut, 0) = 0 then 1L else 0L end), 0L))
			from InterviewQuestionSetEntity i
			where i.jobPostingId = :jobPostingId
			group by i.aiModel
			""")
	List<ModelUsage> usageByModelForJobPostingId(@Param("jobPostingId") UUID jobPostingId);
}
