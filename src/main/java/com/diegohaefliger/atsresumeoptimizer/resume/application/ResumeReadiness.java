package com.diegohaefliger.atsresumeoptimizer.resume.application;

import com.diegohaefliger.atsresumeoptimizer.diagnostics.ReadinessCheck;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.ReadinessContributor;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import java.util.List;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(40)
class ResumeReadiness implements ReadinessContributor {

	private static final String NAME = "Currículo";

	private final ResumeService resumeService;

	ResumeReadiness(ResumeService resumeService) {
		this.resumeService = resumeService;
	}

	@Override
	public List<ReadinessCheck> checks() {
		return List.of(resumeService.hasBaseResume()
				? ReadinessCheck.ok(NAME, "cadastrado")
				: ReadinessCheck.warning(NAME, "nenhum currículo enviado ainda",
						"Envie um currículo em Currículos pra analisar e adaptar para as vagas."));
	}
}
