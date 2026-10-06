package com.diegohaefliger.atsresumeoptimizer.googlecalendar.adapter.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.googlecalendar.application.GoogleCalendarService;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleConnectionStatus;
import com.diegohaefliger.atsresumeoptimizer.googlecalendar.domain.GoogleNotConfiguredException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GoogleCalendarController.class)
@Import(GoogleCalendarWebMapperImpl.class)
class GoogleCalendarControllerTest {

	private static final String BASE = "/api/v1/google-calendar";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GoogleCalendarService service;

	@Test
	void returnsTheStatus() throws Exception {
		when(service.status()).thenReturn(new GoogleConnectionStatus(true, true, "eu@example.com", "http://localhost:8080/cb"));

		mockMvc.perform(get(BASE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.configured").value(true))
				.andExpect(jsonPath("$.connected").value(true))
				.andExpect(jsonPath("$.accountEmail").value("eu@example.com"))
				.andExpect(jsonPath("$.redirectUri").value("http://localhost:8080/cb"));
	}

	@Test
	void givesTheAuthorizationUrlOrExplainsWhyNot() throws Exception {
		when(service.authorizationUrl()).thenReturn("https://accounts.google.com/auth?x=1").thenThrow(new GoogleNotConfiguredException());

		mockMvc.perform(get(BASE + "/authorize"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.url").value("https://accounts.google.com/auth?x=1"));
		mockMvc.perform(get(BASE + "/authorize")).andExpect(status().isBadRequest());
	}

	@Test
	void callbackRedirectsBackToTheApp() throws Exception {
		when(service.completeAuthorization("c0de", "st")).thenReturn("http://localhost:5173/settings/notifications?google=connected");

		mockMvc.perform(get(BASE + "/callback").param("code", "c0de").param("state", "st"))
				.andExpect(status().isFound())
				.andExpect(header().string("Location", "http://localhost:5173/settings/notifications?google=connected"));
	}

	@Test
	void syncsAndDisconnects() throws Exception {
		when(service.syncAll()).thenReturn(3);

		mockMvc.perform(post(BASE + "/sync")).andExpect(status().isOk()).andExpect(jsonPath("$.synced").value(3));
		mockMvc.perform(delete(BASE + "/connection")).andExpect(status().isNoContent());
		verify(service).disconnect();
	}
}
