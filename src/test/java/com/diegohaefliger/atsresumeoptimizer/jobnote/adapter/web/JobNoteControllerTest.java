package com.diegohaefliger.atsresumeoptimizer.jobnote.adapter.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.jobnote.application.JobNoteService;
import com.diegohaefliger.atsresumeoptimizer.jobnote.domain.JobNote;
import com.diegohaefliger.atsresumeoptimizer.jobnote.domain.UnknownNoteJobException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(JobNoteController.class)
@Import(JobNoteWebMapperImpl.class)
class JobNoteControllerTest {

	private static final UUID JOB_ID = UUID.randomUUID();
	private static final String URL = "/api/v1/jobs/" + JOB_ID + "/note";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private JobNoteService service;

	@Test
	void returnsTheNote() throws Exception {
		when(service.get(JOB_ID)).thenReturn(new JobNote(JOB_ID, "falar com o gestor", Instant.now()));

		mockMvc.perform(get(URL))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value("falar com o gestor"));
	}

	@Test
	void savesTheNote() throws Exception {
		when(service.save(JOB_ID, "**empresa** nova")).thenReturn(new JobNote(JOB_ID, "**empresa** nova", Instant.now()));

		mockMvc.perform(put(URL).contentType("application/json").content("{\"content\":\"**empresa** nova\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value("**empresa** nova"));
	}

	@Test
	void rejectsAMissingText() throws Exception {
		mockMvc.perform(put(URL).contentType("application/json").content("{}")).andExpect(status().isBadRequest());
	}

	@Test
	void rejectsATextOverTheLimit() throws Exception {
		String tooLong = "a".repeat(JobNoteRequest.MAX_CONTENT + 1);

		mockMvc.perform(put(URL).contentType("application/json").content("{\"content\":\"" + tooLong + "\"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void answers404ForAnUnknownJob() throws Exception {
		when(service.get(JOB_ID)).thenThrow(new UnknownNoteJobException(JOB_ID));

		mockMvc.perform(get(URL)).andExpect(status().isNotFound());
	}
}
