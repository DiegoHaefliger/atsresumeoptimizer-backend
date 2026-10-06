package com.diegohaefliger.atsresumeoptimizer.selection.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class SelectionProcessNotFoundException extends BusinessException {

	public SelectionProcessNotFoundException(UUID id) {
		super("Processo seletivo não encontrado: %s".formatted(id));
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.NOT_FOUND;
	}
}
