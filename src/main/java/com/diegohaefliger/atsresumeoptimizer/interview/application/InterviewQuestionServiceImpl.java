package com.diegohaefliger.atsresumeoptimizer.interview.application;

import com.diegohaefliger.atsresumeoptimizer.ai.AiPort;
import com.diegohaefliger.atsresumeoptimizer.ai.AiResult;
import com.diegohaefliger.atsresumeoptimizer.ai.InterviewQuestionDraft;
import com.diegohaefliger.atsresumeoptimizer.ai.InterviewQuestionsBrief;
import com.diegohaefliger.atsresumeoptimizer.ai.InterviewQuestionsDraft;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisQueryService;
import com.diegohaefliger.atsresumeoptimizer.interview.domain.InterviewJobNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.interview.domain.InterviewQuestionSet;
import com.diegohaefliger.atsresumeoptimizer.interview.domain.InterviewQuestionsUnavailableException;
import com.diegohaefliger.atsresumeoptimizer.interview.domain.InterviewRequiresJobResumeException;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
class InterviewQuestionServiceImpl implements InterviewQuestionService {

	private static final int QUESTION_COUNT = 10;

	private final InterviewQuestionSetRepository repository;
	private final InterviewQuestionEntityMapper mapper;
	private final AnalysisQueryService analysisQueryService;
	private final JobStructuringService jobStructuringService;
	private final ResumeService resumeService;
	private final AiPort aiPort;
	private final Clock clock;

	InterviewQuestionServiceImpl(InterviewQuestionSetRepository repository, InterviewQuestionEntityMapper mapper,
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
	public List<InterviewQuestionSet> list(UUID jobPostingId) {
		return repository.findByJobPostingIdOrderByCreatedAtDesc(jobPostingId).stream().map(mapper::toDomain).toList();
	}

	@Override
	public InterviewQuestionSet generate(UUID jobPostingId, UUID resumeVersionId) {
		JobOffer job = jobStructuringService.offer(jobPostingId)
				.orElseThrow(() -> new InterviewJobNotFoundException(jobPostingId));
		String resumeText = resumeText(jobPostingId, resumeVersionId);
		AiResult<InterviewQuestionsDraft> draft = aiPort.writeInterviewQuestions(new InterviewQuestionsBrief(job.title(),
				job.company(), job.seniority(), job.rawText(), jobStructuringService.selectedKeywords(jobPostingId),
				jobStructuringService.keywords(jobPostingId), resumeText));
		List<InterviewQuestionDraft> questions = draft.value().questions().stream()
				.filter(question -> StringUtils.hasText(question.question()))
				.limit(QUESTION_COUNT)
				.toList();
		if (questions.isEmpty()) {
			throw new InterviewQuestionsUnavailableException();
		}

		InterviewQuestionSetEntity entity = new InterviewQuestionSetEntity(jobPostingId, resumeVersionId, clock.instant());
		entity.setQuestions(mapper.toValues(questions));
		entity.setAiModel(draft.usage().model());
		entity.setTokensIn(draft.usage().tokensIn());
		entity.setTokensOut(draft.usage().tokensOut());
		entity.setCostUsd(draft.usage().costUsd());
		return mapper.toDomain(repository.save(entity));
	}

	private String resumeText(UUID jobPostingId, UUID resumeVersionId) {
		if (resumeVersionId == null) {
			return null;
		}
		analysisQueryService.analysisOfGeneratedVersion(jobPostingId, resumeVersionId)
				.orElseThrow(() -> new InterviewRequiresJobResumeException(resumeVersionId));
		return resumeService.getVersionText(resumeVersionId).rawText();
	}
}
