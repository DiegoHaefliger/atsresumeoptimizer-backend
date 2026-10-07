package com.diegohaefliger.atsresumeoptimizer.coverletter.application;

import com.diegohaefliger.atsresumeoptimizer.ai.AiPort;
import com.diegohaefliger.atsresumeoptimizer.ai.AiResult;
import com.diegohaefliger.atsresumeoptimizer.ai.CoverLetterBrief;
import com.diegohaefliger.atsresumeoptimizer.ai.CoverLetterDraft;
import com.diegohaefliger.atsresumeoptimizer.analysis.AnalysisSnapshotPort;
import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetter;
import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetterRequiresAdaptedResumeException;
import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetterRequiresJobException;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.scoring.domain.AnalysisMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CoverLetterServiceImpl implements CoverLetterService {

	private final CoverLetterRepository repository;
	private final CoverLetterEntityMapper mapper;
	private final AnalysisSnapshotPort analysisSnapshots;
	private final JobStructuringService jobStructuringService;
	private final ResumeService resumeService;
	private final AiPort aiPort;
	private final Clock clock;

	CoverLetterServiceImpl(CoverLetterRepository repository, CoverLetterEntityMapper mapper,
			AnalysisSnapshotPort analysisSnapshots, JobStructuringService jobStructuringService, ResumeService resumeService,
			AiPort aiPort, Clock clock) {
		this.repository = repository;
		this.mapper = mapper;
		this.analysisSnapshots = analysisSnapshots;
		this.jobStructuringService = jobStructuringService;
		this.resumeService = resumeService;
		this.aiPort = aiPort;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<CoverLetter> find(AnalysisId analysisId) {
		return jobOf(analysisId)
				.flatMap(repository::findByJobPostingId)
				.map(mapper::toDomain);
	}

	@Override
	public CoverLetter generate(AnalysisId analysisId) {
		UUID jobPostingId = jobPostingId(analysisId);
		UUID adaptedVersionId = resumeService.latestAdaptedVersion(analysisId.value())
				.orElseThrow(() -> new CoverLetterRequiresAdaptedResumeException(analysisId));
		JobOffer job = jobStructuringService.offer(jobPostingId)
				.orElseThrow(() -> new CoverLetterRequiresJobException(analysisId));
		AiResult<CoverLetterDraft> draft = aiPort.writeCoverLetter(new CoverLetterBrief(job.title(), job.company(),
				job.rawText(), jobStructuringService.selectedKeywords(jobPostingId),
				resumeService.getVersionText(adaptedVersionId).rawText()));

		CoverLetterEntity entity = upsert(jobPostingId, analysisId);
		entity.setContent(draft.value().text().strip());
		entity.setAiModel(draft.usage().model());
		entity.setTokensIn(draft.usage().tokensIn());
		entity.setTokensOut(draft.usage().tokensOut());
		entity.setCostUsd(draft.usage().costUsd());
		return mapper.toDomain(repository.save(entity));
	}

	@Override
	@Transactional
	public CoverLetter save(AnalysisId analysisId, String content) {
		CoverLetterEntity entity = upsert(jobPostingId(analysisId), analysisId);
		entity.setContent(content.strip());
		return mapper.toDomain(repository.save(entity));
	}

	private UUID jobPostingId(AnalysisId analysisId) {
		return jobOf(analysisId).orElseThrow(() -> new CoverLetterRequiresJobException(analysisId));
	}

	private Optional<UUID> jobOf(AnalysisId analysisId) {
		return analysisSnapshots.find(analysisId)
				.filter(snapshot -> snapshot.mode() == AnalysisMode.JOB_MATCH)
				.flatMap(snapshot -> analysisSnapshots.jobPostingId(analysisId));
	}

	private CoverLetterEntity upsert(UUID jobPostingId, AnalysisId analysisId) {
		Instant now = clock.instant();
		CoverLetterEntity entity =
				repository.findByJobPostingId(jobPostingId).orElseGet(() -> new CoverLetterEntity(jobPostingId, now));
		entity.setAnalysisId(analysisId.value());
		entity.setUpdatedAt(now);
		return entity;
	}
}
