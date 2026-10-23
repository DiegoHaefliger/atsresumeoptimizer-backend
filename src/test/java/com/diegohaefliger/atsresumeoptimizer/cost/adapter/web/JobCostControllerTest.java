package com.diegohaefliger.atsresumeoptimizer.cost.adapter.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.ai.AiStage;
import com.diegohaefliger.atsresumeoptimizer.cost.application.JobCostService;
import com.diegohaefliger.atsresumeoptimizer.cost.domain.CostJobNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.cost.domain.JobCost;
import com.diegohaefliger.atsresumeoptimizer.cost.domain.StageCost;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(JobCostController.class)
@Import(JobCostWebMapperImpl.class)
class JobCostControllerTest {

	private static final UUID JOB_ID = UUID.randomUUID();

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JobCostService service;

	@Test
	void returnsTheCostOfTheJobByStage() throws Exception {
		StageCost analysis = new StageCost(AiStage.JOB_ANALYSIS, true, 140, 70, new BigDecimal("0.0015"),
				new BigDecimal("0.0083"), List.of("gpt-4o-mini"), false, false);
		StageCost adaptation = new StageCost(AiStage.RESUME_ADAPTATION, false, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO,
				List.of(), false, false);
		when(service.cost(JOB_ID)).thenReturn(new JobCost(new BigDecimal("0.0015"), new BigDecimal("0.0083"),
				new BigDecimal("5.50"), List.of("gpt-4o-mini"), false, false, List.of(analysis, adaptation)));

		mockMvc.perform(get("/api/v1/jobs/" + JOB_ID + "/cost"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalCostBrl").value(0.0083))
				.andExpect(jsonPath("$.usdBrlRate").value(5.50))
				.andExpect(jsonPath("$.stages[0].stage").value("JOB_ANALYSIS"))
				.andExpect(jsonPath("$.stages[0].tokensIn").value(140))
				.andExpect(jsonPath("$.stages[1].executed").value(false));
	}

	@Test
	void answersNotFoundForAnUnknownJob() throws Exception {
		when(service.cost(JOB_ID)).thenThrow(new CostJobNotFoundException(JOB_ID));

		mockMvc.perform(get("/api/v1/jobs/" + JOB_ID + "/cost")).andExpect(status().isNotFound());
	}
}
