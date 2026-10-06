package com.diegohaefliger.atsresumeoptimizer.selection.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class UnknownJobPostingException extends BusinessException {

	public UnknownJobPostingException(UUID jobPostingId) {
		super("Cadastre a vaga antes de criar o processo seletivo. Vaga não encontrada: %s".formatted(jobPostingId));
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.BAD_REQUEST;
	}
}
