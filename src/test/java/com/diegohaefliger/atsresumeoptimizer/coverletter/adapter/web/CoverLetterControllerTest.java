package com.diegohaefliger.atsresumeoptimizer.coverletter.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.analysis.domain.AnalysisId;
import com.diegohaefliger.atsresumeoptimizer.coverletter.application.CoverLetterService;
import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetter;
import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetterRequiresAdaptedResumeException;
import com.diegohaefliger.atsresumeoptimizer.coverletter.domain.CoverLetterRequiresJobException;
import java.time.Instant;
import java.util.Optional;
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

	private static final UUID ANALYSIS_ID = UUID.randomUUID();
	private static final UUID JOB_ID = UUID.randomUUID();
	private static final String BASE = "/api/v1/analyses/" + ANALYSIS_ID + "/cover-letter";
	private static final CoverLetter LETTER = new CoverLetter(UUID.randomUUID(), JOB_ID, ANALYSIS_ID, "Texto",
			"gpt-4o-mini", Instant.parse("2026-10-07T12:00:00Z"), Instant.parse("2026-10-07T12:00:00Z"));

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private CoverLetterService service;

	@Test
	void returnsTheSavedCoverLetter() throws Exception {
		when(service.find(new AnalysisId(ANALYSIS_ID))).thenReturn(Optional.of(LETTER));

		mockMvc.perform(get(BASE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value("Texto"))
				.andExpect(jsonPath("$.jobPostingId").value(JOB_ID.toString()));
	}

	@Test
	void answersNoContentWhenThereIsNoCoverLetterYet() throws Exception {
		when(service.find(new AnalysisId(ANALYSIS_ID))).thenReturn(Optional.empty());

		mockMvc.perform(get(BASE)).andExpect(status().isNoContent());
	}

	@Test
	void generatesTheCoverLetter() throws Exception {
		when(service.generate(new AnalysisId(ANALYSIS_ID))).thenReturn(LETTER);

		mockMvc.perform(post(BASE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.aiModel").value("gpt-4o-mini"));
	}

	@Test
	void explainsWhyTheCoverLetterCannotBeGenerated() throws Exception {
		when(service.generate(new AnalysisId(ANALYSIS_ID)))
				.thenThrow(new CoverLetterRequiresAdaptedResumeException(new AnalysisId(ANALYSIS_ID)));

		mockMvc.perform(post(BASE)).andExpect(status().isConflict());
	}

	@Test
	void savesTheEditedText() throws Exception {
		when(service.save(eq(new AnalysisId(ANALYSIS_ID)), eq("Meu texto"))).thenReturn(LETTER);

		mockMvc.perform(put(BASE).contentType("application/json").content("{\"content\":\"Meu texto\"}"))
				.andExpect(status().isOk());
	}

	@Test
	void rejectsAnEmptyText() throws Exception {
		mockMvc.perform(put(BASE).contentType("application/json").content("{\"content\":\" \"}"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(service);
	}

	@Test
	void rejectsAnAnalysisWithoutAJob() throws Exception {
		when(service.save(any(), any())).thenThrow(new CoverLetterRequiresJobException(new AnalysisId(ANALYSIS_ID)));

		mockMvc.perform(put(BASE).contentType("application/json").content("{\"content\":\"Texto\"}"))
				.andExpect(status().isUnprocessableEntity());
	}
}
