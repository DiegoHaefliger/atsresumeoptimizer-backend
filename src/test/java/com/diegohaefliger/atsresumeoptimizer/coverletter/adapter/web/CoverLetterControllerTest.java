package com.diegohaefliger.atsresumeoptimizer.coverletter.adapter.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.coverletter.application.CoverLetterService;
import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetter;
import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetterNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetterRequiresJobResumeException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CoverLetterController.class)
@Import(CoverLetterWebMapperImpl.class)
class CoverLetterControllerTest {

	private static final UUID JOB_ID = UUID.randomUUID();
	private static final UUID VERSION_ID = UUID.randomUUID();
	private static final String BASE = "/api/v1/jobs/" + JOB_ID + "/cover-letters";
	private static final CoverLetter LETTER = new CoverLetter(UUID.randomUUID(), JOB_ID, UUID.randomUUID(), VERSION_ID,
			"Texto", "gpt-4o-mini", Instant.parse("2026-10-07T12:00:00Z"), Instant.parse("2026-10-07T12:00:00Z"));

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CoverLetterService service;

	@Test
	void listsTheCoverLettersOfTheJob() throws Exception {
		when(service.list(JOB_ID)).thenReturn(List.of(LETTER));

		mockMvc.perform(get(BASE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].content").value("Texto"))
				.andExpect(jsonPath("$[0].resumeVersionId").value(VERSION_ID.toString()));
	}

	@Test
	void generatesACoverLetterFromTheChosenResume() throws Exception {
		when(service.generate(JOB_ID, VERSION_ID)).thenReturn(LETTER);

		mockMvc.perform(post(BASE).contentType("application/json").content("{\"resumeVersionId\":\"" + VERSION_ID + "\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.aiModel").value("gpt-4o-mini"));
	}

	@Test
	void requiresTheResumeToGenerate() throws Exception {
		mockMvc.perform(post(BASE).contentType("application/json").content("{}")).andExpect(status().isBadRequest());

		verifyNoInteractions(service);
	}

	@Test
	void explainsWhyTheCoverLetterCannotBeGenerated() throws Exception {
		when(service.generate(JOB_ID, VERSION_ID)).thenThrow(new CoverLetterRequiresJobResumeException(VERSION_ID));

		mockMvc.perform(post(BASE).contentType("application/json").content("{\"resumeVersionId\":\"" + VERSION_ID + "\"}"))
				.andExpect(status().isUnprocessableEntity());
	}

	@Test
	void savesTheEditedText() throws Exception {
		when(service.update(JOB_ID, LETTER.id(), "Meu texto")).thenReturn(LETTER);

		mockMvc.perform(put(BASE + "/" + LETTER.id()).contentType("application/json").content("{\"content\":\"Meu texto\"}"))
				.andExpect(status().isOk());
	}

	@Test
	void rejectsAnEmptyText() throws Exception {
		mockMvc.perform(put(BASE + "/" + LETTER.id()).contentType("application/json").content("{\"content\":\" \"}"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(service);
	}

	@Test
	void answersNotFoundForACoverLetterOfAnotherJob() throws Exception {
		UUID id = UUID.randomUUID();
		when(service.update(JOB_ID, id, "Texto")).thenThrow(new CoverLetterNotFoundException(id));

		mockMvc.perform(put(BASE + "/" + id).contentType("application/json").content("{\"content\":\"Texto\"}"))
				.andExpect(status().isNotFound());
	}
}
