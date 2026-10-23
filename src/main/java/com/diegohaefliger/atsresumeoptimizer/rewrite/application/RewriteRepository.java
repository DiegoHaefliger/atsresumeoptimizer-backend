package com.diegohaefliger.atsresumeoptimizer.rewrite.application;

import java.util.Optional;
import com.diegohaefliger.atsresumeoptimizer.ai.ModelUsage;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface RewriteRepository extends JpaRepository<RewriteEntity, UUID> {

	Optional<RewriteEntity> findFirstByAnalysisIdOrderByCreatedAtDesc(UUID analysisId);

	@Query("""
			select new com.diegohaefliger.atsresumeoptimizer.ai.ModelUsage(
			       r.aiModel, coalesce(sum(r.tokensIn), 0L), coalesce(sum(r.tokensOut), 0L),
			       coalesce(sum(r.costUsd), 0bd), count(r),
			       coalesce(sum(case when coalesce(r.tokensIn, 0) + coalesce(r.tokensOut, 0) = 0 then 1L else 0L end), 0L))
			from RewriteEntity r
			where r.analysisId in :analysisIds
			group by r.aiModel
			""")
	List<ModelUsage> usageByModelForAnalysisIds(@Param("analysisIds") Collection<UUID> analysisIds);
}
