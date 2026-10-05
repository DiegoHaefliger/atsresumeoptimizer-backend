package com.diegohaefliger.atsresumeoptimizer.job.adapter.web;

import com.diegohaefliger.atsresumeoptimizer.job.JobRegistration;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/integrations/jobs")
@Tag(name = "Integração de vagas", description = "Rotas server-to-server, autenticadas por API key.")
@SecurityRequirement(name = "apiKey")
class IntegrationJobsController {

	private final JobStructuringService jobStructuringService;
	private final CreateJobRequestMapper requestMapper;

	IntegrationJobsController(JobStructuringService jobStructuringService, CreateJobRequestMapper requestMapper) {
		this.jobStructuringService = jobStructuringService;
		this.requestMapper = requestMapper;
	}

	@PostMapping
	@Operation(summary = "Cadastra uma vaga",
			description = "Grava a vaga e a estrutura com IA. O mesmo texto devolve o mesmo id; se a IA estiver "
					+ "indisponível a vaga fica gravada sem título e keywords.")
	@ApiResponse(responseCode = "201", description = "Vaga cadastrada")
	@ApiResponse(responseCode = "400", description = "Payload inválido", content = @Content)
	@ApiResponse(responseCode = "401", description = "API key ausente ou inválida", content = @Content)
	ResponseEntity<JobCreatedResponse> create(@Valid @RequestBody CreateJobRequest request) {
		JobRegistration registration =
				jobStructuringService.registerAndStructure(request.text(), requestMapper.toJobDetails(request));
		return ResponseEntity.status(HttpStatus.CREATED).body(requestMapper.toResponse(registration));
	}
}
