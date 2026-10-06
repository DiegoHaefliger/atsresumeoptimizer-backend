package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.scoring.domain.AnalysisMode;
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
			       a.jobPostingId, a.targetRole, a.jobDescription, a.createdAt)
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
}
