package com.diegohaefliger.atsresumeoptimizer.analysis.application;

import com.diegohaefliger.atsresumeoptimizer.analysis.AnalysisSnapshot;
import com.diegohaefliger.atsresumeoptimizer.analysis.AnalysisSnapshotPort;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
class AnalysisSnapshotPortImpl implements AnalysisSnapshotPort {

	private final AnalysisRepository repository;

	AnalysisSnapshotPortImpl(AnalysisRepository repository) {
		this.repository = repository;
	}

	@Override
	public Optional<AnalysisSnapshot> find(AnalysisId id) {
		return repository.findById(id.value())
				.map(analysis -> new AnalysisSnapshot(analysis.resumeVersionId(), analysis.status(),
						analysis.overallScore(), analysis.mode(), analysis.jobDescription(), analysis.targetRole()));
	}

	@Override
	public Optional<UUID> jobPostingId(AnalysisId id) {
		return repository.findById(id.value()).map(AnalysisEntity::jobPostingId);
	}
}
