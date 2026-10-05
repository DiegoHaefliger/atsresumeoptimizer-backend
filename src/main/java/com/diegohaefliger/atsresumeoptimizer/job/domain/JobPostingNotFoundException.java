package com.diegohaefliger.atsresumeoptimizer.job.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class JobPostingNotFoundException extends BusinessException {

	public JobPostingNotFoundException(UUID id) {
		super("job_posting não encontrado: %s".formatted(id));
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.NOT_FOUND;
	}
}
