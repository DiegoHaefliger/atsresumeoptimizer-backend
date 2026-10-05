package com.diegohaefliger.atsresumeoptimizer.job.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.job.JobRegistration;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IntegrationJobsController.class)
@Import(CreateJobRequestMapperImpl.class)
@TestPropertySource(properties = "app.integration.api-key=chave-de-teste")
class IntegrationJobsControllerTest {

	private static final String URL = "/api/v1/integrations/jobs";
	private static final String BODY = "{\"text\":\"Vaga de backend Java\",\"company\":\"Acme\"}";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JobStructuringService jobStructuringService;

	@Test
	void registersTheJobWhenTheApiKeyIsValid() throws Exception {
		UUID jobId = UUID.randomUUID();
		when(jobStructuringService.registerAndStructure(eq("Vaga de backend Java"), any()))
				.thenReturn(new JobRegistration(jobId, "Backend Java", "pleno", List.of("Java")));

		mockMvc.perform(post(URL).header("X-API-Key", "chave-de-teste").contentType("application/json").content(BODY))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(jobId.toString()))
				.andExpect(jsonPath("$.requiredKeywords[0]").value("Java"));
	}

	@Test
	void rejectsARequestWithoutTheApiKey() throws Exception {
		mockMvc.perform(post(URL).contentType("application/json").content(BODY))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));

		verifyNoInteractions(jobStructuringService);
	}

	@Test
	void rejectsARequestWithAWrongApiKey() throws Exception {
		mockMvc.perform(post(URL).header("X-API-Key", "outra").contentType("application/json").content(BODY))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(jobStructuringService);
	}

	@Test
	void rejectsAJobWithoutTextEvenWithAValidApiKey() throws Exception {
		mockMvc.perform(post(URL).header("X-API-Key", "chave-de-teste").contentType("application/json")
				.content("{\"text\":\" \"}"))
				.andExpect(status().isBadRequest());
	}
}
