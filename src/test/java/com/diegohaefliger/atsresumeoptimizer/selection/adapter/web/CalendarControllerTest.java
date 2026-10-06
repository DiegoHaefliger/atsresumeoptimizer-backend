package com.diegohaefliger.atsresumeoptimizer.selection.adapter.web;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.selection.CalendarEvent;
import com.diegohaefliger.atsresumeoptimizer.selection.ScheduleStatus;
import com.diegohaefliger.atsresumeoptimizer.selection.SelectionCalendar;
import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import com.diegohaefliger.atsresumeoptimizer.selection.application.CalendarFeedService;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.InvalidScheduleException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CalendarController.class)
@Import({CalendarWebMapperImpl.class, IcsFeedWriter.class})
class CalendarControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SelectionCalendar calendar;

	@MockitoBean
	private CalendarFeedService feedService;

	private CalendarEvent event() {
		return new CalendarEvent(UUID.randomUUID(), UUID.randomUUID(), "Acme", "Dev Java", SelectionStage.SCREENING,
				ScheduleStatus.SCHEDULED, Instant.parse("2026-10-08T17:00:00Z"), 60, null, null);
	}

	@Test
	void listsTheEventsOfTheRange() throws Exception {
		when(calendar.between(any(), any())).thenReturn(List.of(event()));

		mockMvc.perform(get("/api/v1/calendar/events")
						.param("from", "2026-10-01T00:00:00Z")
						.param("to", "2026-11-01T00:00:00Z"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].company").value("Acme"))
				.andExpect(jsonPath("$[0].stage").value("SCREENING"));
	}

	@Test
	void rejectsAnInvalidRange() throws Exception {
		when(calendar.between(any(), any())).thenThrow(new InvalidScheduleException("Intervalo inválido."));

		mockMvc.perform(get("/api/v1/calendar/events")
						.param("from", "2026-11-01T00:00:00Z")
						.param("to", "2026-10-01T00:00:00Z"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/v1/calendar/events")).andExpect(status().isBadRequest());
	}

	@Test
	void servesTheFeedOnlyWithTheRightToken() throws Exception {
		when(calendar.between(any(), any())).thenReturn(List.of(event()));
		when(feedService.accepts("segredo")).thenReturn(true);

		mockMvc.perform(get("/api/v1/calendar/feed.ics").param("token", "segredo"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/calendar"))
				.andExpect(content().string(startsWith("BEGIN:VCALENDAR")));
		mockMvc.perform(get("/api/v1/calendar/feed.ics").param("token", "errado")).andExpect(status().isNotFound());
	}

	@Test
	void exposesWhereToSubscribeOnlyWhenTheFeedIsEnabled() throws Exception {
		when(feedService.token()).thenReturn(Optional.of("segredo")).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/calendar/feed-info"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.enabled").value(true))
				.andExpect(jsonPath("$.path").value("/api/v1/calendar/feed.ics?token=segredo"));
		mockMvc.perform(get("/api/v1/calendar/feed-info"))
				.andExpect(jsonPath("$.enabled").value(false))
				.andExpect(jsonPath("$.path").doesNotExist());
	}

	@Test
	void generatesANewTokenAndDisablesTheFeed() throws Exception {
		when(feedService.regenerate()).thenReturn("novo");

		mockMvc.perform(post("/api/v1/calendar/feed/token"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.path").value("/api/v1/calendar/feed.ics?token=novo"));
		mockMvc.perform(delete("/api/v1/calendar/feed/token")).andExpect(status().isNoContent());
		verify(feedService).disable();
	}
}
