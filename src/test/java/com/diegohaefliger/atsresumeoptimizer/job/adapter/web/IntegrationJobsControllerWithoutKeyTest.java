package com.diegohaefliger.atsresumeoptimizer.job.adapter.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IntegrationJobsController.class)
@Import(CreateJobRequestMapperImpl.class)
@TestPropertySource(properties = "app.integration.api-key=")
class IntegrationJobsControllerWithoutKeyTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JobStructuringService jobStructuringService;

	@Test
	void closesTheRouteWhenNoKeyIsConfiguredOnTheServer() throws Exception {
		mockMvc.perform(post("/api/v1/integrations/jobs").header("X-API-Key", "").contentType("application/json")
				.content("{\"text\":\"Vaga\"}"))
				.andExpect(status().isUnauthorized());
	}
}
