package com.diegohaefliger.atsresumeoptimizer.jobsearch.adapter.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.JobSuggestionService;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.InvalidSuggestionStateException;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.JobSuggestion;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.JobSuggestionNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionStatus;

@WebMvcTest(JobSuggestionController.class)
@Import(JobSearchWebMapperImpl.class)
class JobSuggestionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JobSuggestionService suggestionService;

	private static JobSuggestion suggestion(UUID id, SuggestionStatus status) {
		return new JobSuggestion(id, "LINKEDIN", "111", "https://www.linkedin.com/jobs/view/111", "Dev Java", "Acme",
				"Curitiba, Brazil", LocalDate.of(2026, 10, 23), "Descrição", 85, status,
				Instant.parse("2026-10-24T12:00:00Z"), null, null);
	}

	@Test
	void listsTheNewSuggestions() throws Exception {
		UUID id = UUID.randomUUID();
		when(suggestionService.newSuggestions()).thenReturn(List.of(suggestion(id, SuggestionStatus.NEW)));

		mockMvc.perform(get("/api/v1/job-suggestions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(id.toString()))
				.andExpect(jsonPath("$[0].title").value("Dev Java"))
				.andExpect(jsonPath("$[0].score").value(85))
				.andExpect(jsonPath("$[0].status").value("NEW"));
	}

	@Test
	void listsTheSuggestionsBelowTheMinimumAdherence() throws Exception {
		UUID id = UUID.randomUUID();
		when(suggestionService.belowAdherence()).thenReturn(List.of(suggestion(id, SuggestionStatus.FILTERED)));

		mockMvc.perform(get("/api/v1/job-suggestions/below-adherence"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(id.toString()))
				.andExpect(jsonPath("$[0].status").value("FILTERED"));
	}

	@Test
	void countsTheNewSuggestions() throws Exception {
		when(suggestionService.countNew()).thenReturn(4L);

		mockMvc.perform(get("/api/v1/job-suggestions/count"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.count").value(4));
	}

	@Test
	void promotesASuggestionAndReturnsTheNewJobId() throws Exception {
		UUID id = UUID.randomUUID();
		UUID jobId = UUID.randomUUID();
		when(suggestionService.promote(id)).thenReturn(jobId);

		mockMvc.perform(post("/api/v1/job-suggestions/{id}/promote", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.jobId").value(jobId.toString()));
	}

	@Test
	void discardsASuggestion() throws Exception {
		UUID id = UUID.randomUUID();
		when(suggestionService.discard(id)).thenReturn(suggestion(id, SuggestionStatus.DISCARDED));

		mockMvc.perform(post("/api/v1/job-suggestions/{id}/discard", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DISCARDED"));
	}

	@Test
	void answersConflictWhenDiscardingAPromotedSuggestion() throws Exception {
		UUID id = UUID.randomUUID();
		when(suggestionService.discard(id)).thenThrow(new InvalidSuggestionStateException());

		mockMvc.perform(post("/api/v1/job-suggestions/{id}/discard", id)).andExpect(status().isConflict());
	}

	@Test
	void answersNotFoundForAnUnknownSuggestion() throws Exception {
		UUID id = UUID.randomUUID();
		when(suggestionService.promote(id)).thenThrow(new JobSuggestionNotFoundException());

		mockMvc.perform(post("/api/v1/job-suggestions/{id}/promote", id)).andExpect(status().isNotFound());
	}
}
