package com.diegohaefliger.atsresumeoptimizer.coverletter.application;

import com.diegohaefliger.atsresumeoptimizer.ai.AiPort;
import com.diegohaefliger.atsresumeoptimizer.ai.AiResult;
import com.diegohaefliger.atsresumeoptimizer.ai.CoverLetterBrief;
import com.diegohaefliger.atsresumeoptimizer.ai.CoverLetterDraft;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisQueryService;
import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetter;
import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetterJobNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetterNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetterRequiresJobResumeException;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CoverLetterServiceImpl implements CoverLetterService {

	private final CoverLetterRepository repository;
	private final CoverLetterEntityMapper mapper;
	private final AnalysisQueryService analysisQueryService;
	private final JobStructuringService jobStructuringService;
	private final ResumeService resumeService;
	private final AiPort aiPort;
	private final Clock clock;

	CoverLetterServiceImpl(CoverLetterRepository repository, CoverLetterEntityMapper mapper,
			AnalysisQueryService analysisQueryService, JobStructuringService jobStructuringService,
			ResumeService resumeService, AiPort aiPort, Clock clock) {
		this.repository = repository;
		this.mapper = mapper;
		this.analysisQueryService = analysisQueryService;
		this.jobStructuringService = jobStructuringService;
		this.resumeService = resumeService;
		this.aiPort = aiPort;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public List<CoverLetter> list(UUID jobPostingId) {
		return repository.findByJobPostingIdOrderByCreatedAtDesc(jobPostingId).stream().map(mapper::toDomain).toList();
	}

	@Override
	public CoverLetter generate(UUID jobPostingId, UUID resumeVersionId) {
		JobOffer job = jobStructuringService.offer(jobPostingId)
				.orElseThrow(() -> new CoverLetterJobNotFoundException(jobPostingId));
		UUID analysisId = analysisQueryService.analysisOfGeneratedVersion(jobPostingId, resumeVersionId)
				.orElseThrow(() -> new CoverLetterRequiresJobResumeException(resumeVersionId));
		AiResult<CoverLetterDraft> draft = aiPort.writeCoverLetter(new CoverLetterBrief(job.title(), job.company(),
				job.rawText(), jobStructuringService.selectedKeywords(jobPostingId),
				resumeService.getVersionText(resumeVersionId).rawText()));

		CoverLetterEntity entity = new CoverLetterEntity(jobPostingId, clock.instant());
		entity.setAnalysisId(analysisId);
		entity.setResumeVersionId(resumeVersionId);
		entity.setContent(draft.value().text().strip());
		entity.setAiModel(draft.usage().model());
		entity.setTokensIn(draft.usage().tokensIn());
		entity.setTokensOut(draft.usage().tokensOut());
		entity.setCostUsd(draft.usage().costUsd());
		return mapper.toDomain(repository.save(entity));
	}

	@Override
	@Transactional
	public CoverLetter update(UUID jobPostingId, UUID coverLetterId, String content) {
		CoverLetterEntity entity = repository.findByIdAndJobPostingId(coverLetterId, jobPostingId)
				.orElseThrow(() -> new CoverLetterNotFoundException(coverLetterId));
		Instant now = clock.instant();
		entity.setContent(content.strip());
		entity.setUpdatedAt(now);
		return mapper.toDomain(repository.save(entity));
	}
}
