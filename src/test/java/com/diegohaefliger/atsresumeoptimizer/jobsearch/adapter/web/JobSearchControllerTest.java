package com.diegohaefliger.atsresumeoptimizer.jobsearch.adapter.web;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.JobSearchRunService;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.RunTrigger;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SearchAlreadyRunningException;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.IncompleteSearchCriteriaException;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SearchRun;

@WebMvcTest(JobSearchController.class)
@Import(JobSearchWebMapperImpl.class)
class JobSearchControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JobSearchRunService runService;

	@Test
	void startsARunAndAnswersAcceptedWithoutWaiting() throws Exception {
		mockMvc.perform(post("/api/v1/job-search/run")).andExpect(status().isAccepted());

		verify(runService).startManual();
	}

	@Test
	void answersUnprocessableWhenThePreferencesAreIncomplete() throws Exception {
		doThrow(new IncompleteSearchCriteriaException(java.util.List.of("Senioridade", "Localização")))
				.when(runService).startManual();

		mockMvc.perform(post("/api/v1/job-search/run"))
				.andExpect(status().isUnprocessableEntity())
				.andExpect(jsonPath("$.detail").value("Preencha nas Preferências antes de buscar: Senioridade, Localização."));
	}

	@Test
	void answersConflictWhenARunIsAlreadyInProgress() throws Exception {
		doThrow(new SearchAlreadyRunningException()).when(runService).startManual();

		mockMvc.perform(post("/api/v1/job-search/run"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail").value("Já tem uma busca em andamento."));
	}

	@Test
	void reportsTheStatusWithTheLastRun() throws Exception {
		when(runService.running()).thenReturn(true);
		when(runService.lastRun()).thenReturn(Optional.of(new SearchRun(UUID.randomUUID(), RunTrigger.SCHEDULED,
				Instant.parse("2026-10-24T11:00:00Z"), Instant.parse("2026-10-24T11:05:00Z"), 20, 4, 10, 6, null)));

		mockMvc.perform(get("/api/v1/job-search/status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.running").value(true))
				.andExpect(jsonPath("$.lastRun.trigger").value("SCHEDULED"))
				.andExpect(jsonPath("$.lastRun.added").value(4))
				.andExpect(jsonPath("$.lastRun.belowAdherence").value(6));
	}

	@Test
	void reportsNoLastRunBeforeTheFirstSearch() throws Exception {
		when(runService.lastRun()).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/job-search/status"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.running").value(false))
				.andExpect(jsonPath("$.lastRun").doesNotExist());
	}
}
