package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.scoring.domain.AnalysisMode;
import com.diegohaefliger.atsresumeoptimizer.ai.ModelUsage;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AnalysisRepository extends JpaRepository<AnalysisEntity, UUID> {

	@Query("""
			select a.aiModel as model, count(a) as analysisCount, coalesce(sum(a.costUsd), 0) as totalCostUsd,
			       coalesce(avg(a.costUsd), 0) as avgCostUsd
			from AnalysisEntity a
			where a.aiModel is not null
			group by a.aiModel
			""")
	List<CostByModelProjection> aggregateCostByModel();

	@Query("select a.id from AnalysisEntity a where a.jobPostingId = :jobPostingId")
	List<UUID> findIdsByJobPostingId(@Param("jobPostingId") UUID jobPostingId);

	@Query("""
			select new com.diegohaefliger.atsresumeoptimizer.analysis.application.RecentJobInput(
			       a.jobPostingId, a.targetRole, a.createdAt)
			from AnalysisEntity a
			where a.mode = :mode
			  and a.jobPostingId is not null
			  and a.createdAt = (select max(b.createdAt) from AnalysisEntity b where b.jobPostingId = a.jobPostingId)
			order by a.createdAt desc
			""")
	List<RecentJobInput> findLatestJobInputsByMode(@Param("mode") AnalysisMode mode);

	@Query("""
			select new com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisJobRef(a.id, a.jobPostingId)
			from AnalysisEntity a
			where a.jobPostingId is not null
			""")
	List<AnalysisJobRef> findAllJobRefs();

	@Query("""
			select new com.diegohaefliger.atsresumeoptimizer.ai.ModelUsage(
			       a.aiModel, coalesce(sum(a.tokensIn), 0L), coalesce(sum(a.tokensOut), 0L),
			       coalesce(sum(a.costUsd), 0bd), count(a),
			       coalesce(sum(case when coalesce(a.tokensIn, 0) + coalesce(a.tokensOut, 0) = 0 then 1L else 0L end), 0L))
			from AnalysisEntity a
			where a.jobPostingId = :jobPostingId
			group by a.aiModel
			""")
	List<ModelUsage> usageByModelForJobPostingId(@Param("jobPostingId") UUID jobPostingId);
}
