package com.diegohaefliger.atsresumeoptimizer.jobsearch.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.PostImportService;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.JobSuggestion;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.PostAlreadyImportedException;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.PostTextUnavailableException;
import com.diegohaefliger.atsresumeoptimizer.jobsearch.domain.SuggestionStatus;

@WebMvcTest(PostImportController.class)
@Import(JobSearchWebMapperImpl.class)
class PostImportControllerTest {

	private static final String LINK = "https://www.linkedin.com/posts/fulano_vaga-activity-7251234567890123456-AbCd";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PostImportService importService;

	private static String body(String text) {
		return "{\"url\":\"" + LINK + "\"" + (text == null ? "" : ",\"text\":\"" + text + "\"") + "}";
	}

	@Test
	void importsAPostAndReturnsTheSuggestion() throws Exception {
		UUID id = UUID.randomUUID();
		when(importService.importPost(eq(LINK), any())).thenReturn(new JobSuggestion(id, "LINKEDIN_POST",
				"7251234567890123456", LINK, "Estamos contratando", null, null, null, "texto", 70,
				SuggestionStatus.NEW, Instant.parse("2026-10-24T12:00:00Z"), null, null));

		mockMvc.perform(post("/api/v1/job-suggestions/import").contentType(MediaType.APPLICATION_JSON).content(body("oi")))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.source").value("LINKEDIN_POST"));
	}

	@Test
	void rejectsABlankLink() throws Exception {
		mockMvc.perform(post("/api/v1/job-suggestions/import").contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"\"}")).andExpect(status().isBadRequest());
	}

	@Test
	void answers422WhenThePostCannotBeRead() throws Exception {
		when(importService.importPost(eq(LINK), any())).thenThrow(new PostTextUnavailableException());

		mockMvc.perform(post("/api/v1/job-suggestions/import").contentType(MediaType.APPLICATION_JSON).content(body(null)))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void answers409WhenThePostWasAlreadyImported() throws Exception {
		when(importService.importPost(eq(LINK), any())).thenThrow(new PostAlreadyImportedException());

		mockMvc.perform(post("/api/v1/job-suggestions/import").contentType(MediaType.APPLICATION_JSON).content(body(null)))
				.andExpect(status().isConflict());
	}
}
