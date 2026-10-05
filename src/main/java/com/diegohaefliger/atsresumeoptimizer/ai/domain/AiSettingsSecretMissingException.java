package com.diegohaefliger.atsresumeoptimizer.ai.domain;

import com.diegohaefliger.atsresumeoptimizer.BusinessException;
import org.springframework.http.HttpStatus;

public class AiSettingsSecretMissingException extends BusinessException {

	public AiSettingsSecretMissingException() {
		super("Defina a variável AI_SETTINGS_SECRET no backend antes de salvar uma chave de API.");
	}

	@Override
	public HttpStatus status() {
		return HttpStatus.CONFLICT;
	}
}
