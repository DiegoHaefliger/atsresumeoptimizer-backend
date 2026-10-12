package com.diegohaefliger.atsresumeoptimizer.interview.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class InterviewJobNotFoundException extends BusinessException {

	public InterviewJobNotFoundException(UUID jobPostingId) {
		super("Vaga não encontrada: %s".formatted(jobPostingId));
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.NOT_FOUND;
	}
}
