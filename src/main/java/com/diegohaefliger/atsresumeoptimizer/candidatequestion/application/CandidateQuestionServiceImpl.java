package com.diegohaefliger.atsresumeoptimizer.candidatequestion.application;

import com.diegohaefliger.atsresumeoptimizer.ai.AiPort;
import com.diegohaefliger.atsresumeoptimizer.ai.AiResult;
import com.diegohaefliger.atsresumeoptimizer.ai.CandidateQuestionDraft;
import com.diegohaefliger.atsresumeoptimizer.ai.CandidateQuestionsBrief;
import com.diegohaefliger.atsresumeoptimizer.ai.CandidateQuestionsDraft;
import com.diegohaefliger.atsresumeoptimizer.candidatequestion.domain.CandidateQuestionSet;
import com.diegohaefliger.atsresumeoptimizer.candidatequestion.domain.CandidateQuestionsJobNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.candidatequestion.domain.CandidateQuestionsUnavailableException;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
class CandidateQuestionServiceImpl implements CandidateQuestionService {

	private static final int MAX_QUESTIONS = 7;

	private final CandidateQuestionSetRepository repository;
	private final CandidateQuestionEntityMapper mapper;
	private final JobStructuringService jobStructuringService;
	private final AiPort aiPort;
	private final Clock clock;

	CandidateQuestionServiceImpl(CandidateQuestionSetRepository repository, CandidateQuestionEntityMapper mapper,
			JobStructuringService jobStructuringService, AiPort aiPort, Clock clock) {
		this.repository = repository;
		this.mapper = mapper;
		this.jobStructuringService = jobStructuringService;
		this.aiPort = aiPort;
		this.clock = clock;
	}

	@Override
	@Transactional(readOnly = true)
	public List<CandidateQuestionSet> list(UUID jobPostingId) {
		return repository.findByJobPostingIdOrderByCreatedAtDesc(jobPostingId).stream().map(mapper::toDomain).toList();
	}

	@Override
	public CandidateQuestionSet generate(UUID jobPostingId) {
		JobOffer job = jobStructuringService.offer(jobPostingId)
				.orElseThrow(() -> new CandidateQuestionsJobNotFoundException(jobPostingId));
		AiResult<CandidateQuestionsDraft> draft = aiPort.writeCandidateQuestions(
				new CandidateQuestionsBrief(job.title(), job.company(), job.seniority(), job.rawText(), job.benefits()));
		List<CandidateQuestionDraft> questions = draft.value().questions().stream()
				.filter(question -> StringUtils.hasText(question.question()))
				.limit(MAX_QUESTIONS)
				.toList();
		if (questions.isEmpty()) {
			throw new CandidateQuestionsUnavailableException();
		}

		CandidateQuestionSetEntity entity = new CandidateQuestionSetEntity(jobPostingId, clock.instant());
		entity.setQuestions(mapper.toValues(questions));
		entity.setAiModel(draft.usage().model());
		entity.setTokensIn(draft.usage().tokensIn());
		entity.setTokensOut(draft.usage().tokensOut());
		entity.setCostUsd(draft.usage().costUsd());
		return mapper.toDomain(repository.save(entity));
	}
}
