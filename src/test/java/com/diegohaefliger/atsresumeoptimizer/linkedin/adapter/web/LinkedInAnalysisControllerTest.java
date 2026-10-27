package com.diegohaefliger.atsresumeoptimizer.linkedin.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.linkedin.application.LinkedInAnalysisService;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInAnalysis;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInAnalysisReport;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInAnalysisUnavailableException;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSectionReview;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.LinkedInSuggestion;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.ProfileSection;
import com.diegohaefliger.atsresumeoptimizer.linkedin.domain.SuggestionPriority;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LinkedInAnalysisController.class)
@Import(LinkedInAnalysisWebMapperImpl.class)
class LinkedInAnalysisControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LinkedInAnalysisService service;

	private static LinkedInAnalysis analysis(UUID snapshotId) {
		var report = new LinkedInAnalysisReport("Falta posicionamento.", List.of("Reescrever o título"), List.of("Java"),
				List.of("Liderança"), List.of(new LinkedInSectionReview(ProfileSection.HEADLINE, 55, "Genérico.",
						List.of(new LinkedInSuggestion(SuggestionPriority.HIGH, "Incluir o cargo-alvo")), "Tech Lead Java", null)));
		return new LinkedInAnalysis(UUID.randomUUID(), snapshotId, Instant.parse("2026-10-27T12:00:00Z"), 62,
				"Perfil razoável.", report, true, "gpt-4o-mini", new BigDecimal("0.0123"));
	}

	@Test
	void analyzesTheChosenSnapshot() throws Exception {
		UUID snapshotId = UUID.randomUUID();
		when(service.analyze(snapshotId)).thenReturn(analysis(snapshotId));

		mockMvc.perform(post("/api/v1/linkedin/analyses").contentType("application/json")
						.content("{\"snapshotId\":\"" + snapshotId + "\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.overallScore").value(62))
				.andExpect(jsonPath("$.targetFit").value("Falta posicionamento."))
				.andExpect(jsonPath("$.topActions[0]").value("Reescrever o título"))
				.andExpect(jsonPath("$.sections[0].section").value("HEADLINE"))
				.andExpect(jsonPath("$.sections[0].suggestions[0].priority").value("HIGH"))
				.andExpect(jsonPath("$.sections[0].rewrite").value("Tech Lead Java"))
				.andExpect(jsonPath("$.imagesAnalyzed").value(true));
	}

	@Test
	void analyzesTheLatestSnapshotWhenNoBodyIsSent() throws Exception {
		when(service.analyze(any())).thenReturn(analysis(UUID.randomUUID()));

		mockMvc.perform(post("/api/v1/linkedin/analyses")).andExpect(status().isCreated());

		verify(service).analyze(eq(null));
	}

	@Test
	void reportsABadGatewayWhenTheAiFails() throws Exception {
		when(service.analyze(any())).thenThrow(new LinkedInAnalysisUnavailableException("Nenhum provedor de IA configurado."));

		mockMvc.perform(post("/api/v1/linkedin/analyses"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.detail").value("Nenhum provedor de IA configurado."));
	}

	@Test
	void returnsNotFoundWhenThereIsNoAnalysisYet() throws Exception {
		when(service.latest()).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/linkedin/analyses/latest")).andExpect(status().isNotFound());
	}
}
