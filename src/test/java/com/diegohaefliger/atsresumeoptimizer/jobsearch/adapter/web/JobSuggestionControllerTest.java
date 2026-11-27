package com.diegohaefliger.atsresumeoptimizer.jobsearch.adapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.JobSuggestionService;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.InvalidSuggestionStateException;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.JobSuggestion;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.JobSuggestionNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionCounts;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionPage;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionQuery;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionSort;
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

	private static SuggestionPage pageOf(JobSuggestion suggestion) {
		return new SuggestionPage(List.of(suggestion), 0, 30, 41, 2, List.of("GUPY", "LINKEDIN"));
	}

	@Test
	void listsTheFirstPageOfNewSuggestionsWithThirtyItemsByDefault() throws Exception {
		UUID id = UUID.randomUUID();
		when(suggestionService.newSuggestions(any())).thenReturn(pageOf(suggestion(id, SuggestionStatus.NEW)));

		mockMvc.perform(get("/api/v1/job-suggestions"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].id").value(id.toString()))
				.andExpect(jsonPath("$.items[0].title").value("Dev Java"))
				.andExpect(jsonPath("$.items[0].score").value(85))
				.andExpect(jsonPath("$.items[0].status").value("NEW"))
				.andExpect(jsonPath("$.totalItems").value(41))
				.andExpect(jsonPath("$.totalPages").value(2))
				.andExpect(jsonPath("$.sources[0]").value("GUPY"));

		ArgumentCaptor<SuggestionQuery> query = ArgumentCaptor.forClass(SuggestionQuery.class);
		verify(suggestionService).newSuggestions(query.capture());
		assertThat(query.getValue()).isEqualTo(new SuggestionQuery(null, null, null, null, SuggestionSort.SCORE_DESC, 0, 30));
	}

	@Test
	void passesFiltersSortAndPageToTheService() throws Exception {
		UUID id = UUID.randomUUID();
		when(suggestionService.belowAdherence(any())).thenReturn(pageOf(suggestion(id, SuggestionStatus.FILTERED)));

		mockMvc.perform(get("/api/v1/job-suggestions/below-adherence")
						.param("page", "2").param("size", "10").param("query", "java remoto").param("source", "GUPY")
						.param("minScore", "70").param("postedWithinDays", "7").param("sort", "POSTED_DESC"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].status").value("FILTERED"));

		ArgumentCaptor<SuggestionQuery> query = ArgumentCaptor.forClass(SuggestionQuery.class);
		verify(suggestionService).belowAdherence(query.capture());
		assertThat(query.getValue())
				.isEqualTo(new SuggestionQuery("java remoto", "GUPY", 70, 7, SuggestionSort.POSTED_DESC, 2, 10));
	}

	@Test
	void rejectsAPageSizeAboveTheLimit() throws Exception {
		mockMvc.perform(get("/api/v1/job-suggestions/discarded").param("size", "500")).andExpect(status().isBadRequest());
	}

	@Test
	void countsTheSuggestionsOfEachTab() throws Exception {
		when(suggestionService.counts()).thenReturn(new SuggestionCounts(4, 2, 1));

		mockMvc.perform(get("/api/v1/job-suggestions/count"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.newSuggestions").value(4))
				.andExpect(jsonPath("$.belowAdherence").value(2))
				.andExpect(jsonPath("$.discarded").value(1));
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
