package com.diegohaefliger.atsresumeoptimizer.candidatequestion.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CandidateQuestionsJobNotFoundException extends BusinessException {

	public CandidateQuestionsJobNotFoundException(UUID jobPostingId) {
		super("Vaga não encontrada: %s".formatted(jobPostingId));
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.NOT_FOUND;
	}
}
