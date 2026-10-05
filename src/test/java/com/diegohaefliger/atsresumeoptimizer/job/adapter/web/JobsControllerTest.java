package com.diegohaefliger.atsresumeoptimizer.job.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(JobsController.class)
@Import(CreateJobRequestMapperImpl.class)
class JobsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JobStructuringService jobStructuringService;

	@Test
	void registersTheJobAndReturnsWhatTheAiExtracted() throws Exception {
		UUID jobId = UUID.randomUUID();
		when(jobStructuringService.registerAndStructure(eq("Vaga de backend Java"), any()))
				.thenReturn(new JobRegistration(jobId, "Backend Java", "pleno", List.of("Java")));

		mockMvc.perform(post("/api/v1/jobs").contentType("application/json")
				.content("{\"text\":\"Vaga de backend Java\",\"company\":\"Acme\",\"workModel\":\"REMOTE\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(jobId.toString()))
				.andExpect(jsonPath("$.title").value("Backend Java"));
	}

	@Test
	void rejectsAJobWithoutText() throws Exception {
		mockMvc.perform(post("/api/v1/jobs").contentType("application/json").content("{\"text\":\" \"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void updatesTheJob() throws Exception {
		UUID jobId = UUID.randomUUID();
		when(jobStructuringService.update(eq(jobId), eq("Vaga nova"), any()))
				.thenReturn(new JobRegistration(jobId, "Backend Java", "pleno", List.of()));

		mockMvc.perform(put("/api/v1/jobs/{id}", jobId).contentType("application/json")
				.content("""
						{"text":"Vaga nova","workModel":"HYBRID"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Backend Java"));
	}
}
