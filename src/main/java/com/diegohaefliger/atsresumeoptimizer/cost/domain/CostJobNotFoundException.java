package com.diegohaefliger.atsresumeoptimizer.cost.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class CostJobNotFoundException extends BusinessException {

	public CostJobNotFoundException(UUID jobPostingId) {
		super("Vaga não encontrada: %s".formatted(jobPostingId));
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.NOT_FOUND;
	}
}
