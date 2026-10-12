package com.diegohaefliger.atsresumeoptimizer.coverletter.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CoverLetterJobNotFoundException extends BusinessException {

	public CoverLetterJobNotFoundException(UUID jobPostingId) {
		super("Vaga não encontrada: %s".formatted(jobPostingId));
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.NOT_FOUND;
	}
}
