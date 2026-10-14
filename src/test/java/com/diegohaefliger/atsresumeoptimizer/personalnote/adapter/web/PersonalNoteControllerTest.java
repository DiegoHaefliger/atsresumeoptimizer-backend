package com.diegohaefliger.atsresumeoptimizer.personalnote.adapter.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.personalnote.application.PersonalNoteService;
import com.diegohaefliger.atsresumeoptimizer.personalnote.domain.PersonalNote;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PersonalNoteController.class)
@Import(PersonalNoteWebMapperImpl.class)
class PersonalNoteControllerTest {

	private static final String URL = "/api/v1/personal-note";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PersonalNoteService service;

	@Test
	void returnsTheNote() throws Exception {
		when(service.get()).thenReturn(new PersonalNote("**pontual**", Instant.parse("2026-10-14T12:00:00Z")));

		mockMvc.perform(get(URL))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value("**pontual**"));
	}

	@Test
	void savesTheNote() throws Exception {
		when(service.save("- comunicativo")).thenReturn(new PersonalNote("- comunicativo", Instant.now()));

		mockMvc.perform(put(URL).contentType("application/json").content("{\"content\":\"- comunicativo\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value("- comunicativo"));
	}

	@Test
	void rejectsAMissingText() throws Exception {
		mockMvc.perform(put(URL).contentType("application/json").content("{}")).andExpect(status().isBadRequest());
	}

	@Test
	void rejectsATextOverTheLimit() throws Exception {
		String tooLong = "a".repeat(PersonalNoteRequest.MAX_CONTENT + 1);

		mockMvc.perform(put(URL).contentType("application/json").content("{\"content\":\"" + tooLong + "\"}"))
				.andExpect(status().isBadRequest());
	}
}
