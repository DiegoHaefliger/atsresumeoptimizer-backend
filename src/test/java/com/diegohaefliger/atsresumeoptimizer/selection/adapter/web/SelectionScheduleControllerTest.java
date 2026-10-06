package com.diegohaefliger.atsresumeoptimizer.selection.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.selection.ScheduleStatus;
import com.diegohaefliger.atsresumeoptimizer.selection.SelectionStage;
import com.diegohaefliger.atsresumeoptimizer.selection.application.SelectionScheduleService;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.InvalidScheduleException;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.ScheduleNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.selection.domain.SelectionSchedule;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SelectionScheduleController.class)
@Import(SelectionScheduleWebMapperImpl.class)
class SelectionScheduleControllerTest {

	private static final UUID PROCESS_ID = UUID.randomUUID();
	private static final UUID SCHEDULE_ID = UUID.randomUUID();
	private static final Instant AT = Instant.parse("2026-10-08T17:00:00Z");
	private static final String BASE = "/api/v1/selection-processes/" + PROCESS_ID + "/schedules";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SelectionScheduleService service;

	private SelectionSchedule schedule(ScheduleStatus status) {
		return new SelectionSchedule(SCHEDULE_ID, PROCESS_ID, SelectionStage.TECHNICAL_INTERVIEW, AT, 60,
				"https://meet.example/abc", null, status, AT, AT);
	}

	@Test
	void createsASchedule() throws Exception {
		when(service.schedule(eq(PROCESS_ID), any())).thenReturn(schedule(ScheduleStatus.SCHEDULED));

		mockMvc.perform(post(BASE)
						.contentType("application/json")
						.content("{\"scheduledAt\":\"2026-10-08T17:00:00Z\",\"durationMinutes\":60}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(SCHEDULE_ID.toString()))
				.andExpect(jsonPath("$.status").value("SCHEDULED"))
				.andExpect(jsonPath("$.scheduledAt").value("2026-10-08T17:00:00Z"));
	}

	@Test
	void rejectsAScheduleWithoutDateOrWithAnAbsurdDuration() throws Exception {
		mockMvc.perform(post(BASE).contentType("application/json").content("{}")).andExpect(status().isBadRequest());
		mockMvc.perform(post(BASE)
						.contentType("application/json")
						.content("{\"scheduledAt\":\"2026-10-08T17:00:00Z\",\"durationMinutes\":99999}"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(service);
	}

	@Test
	void reschedulesCompletesAndCancels() throws Exception {
		when(service.reschedule(eq(PROCESS_ID), eq(SCHEDULE_ID), any())).thenReturn(schedule(ScheduleStatus.SCHEDULED));
		when(service.complete(PROCESS_ID, SCHEDULE_ID)).thenReturn(schedule(ScheduleStatus.DONE));
		when(service.cancel(PROCESS_ID, SCHEDULE_ID)).thenReturn(schedule(ScheduleStatus.CANCELED));

		mockMvc.perform(post(BASE + "/" + SCHEDULE_ID + "/reschedule")
						.contentType("application/json")
						.content("{\"scheduledAt\":\"2026-10-09T17:00:00Z\"}"))
				.andExpect(status().isOk());
		mockMvc.perform(post(BASE + "/" + SCHEDULE_ID + "/complete"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("DONE"));
		mockMvc.perform(post(BASE + "/" + SCHEDULE_ID + "/cancel"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("CANCELED"));
	}

	@Test
	void answersNotFoundAndBadRequestFromBusinessErrors() throws Exception {
		when(service.complete(PROCESS_ID, SCHEDULE_ID)).thenThrow(new ScheduleNotFoundException(SCHEDULE_ID));
		when(service.cancel(PROCESS_ID, SCHEDULE_ID)).thenThrow(new InvalidScheduleException("Só agendamentos ativos."));

		mockMvc.perform(post(BASE + "/" + SCHEDULE_ID + "/complete")).andExpect(status().isNotFound());
		mockMvc.perform(post(BASE + "/" + SCHEDULE_ID + "/cancel")).andExpect(status().isBadRequest());
	}

	@Test
	void listsTheHistory() throws Exception {
		when(service.list(PROCESS_ID)).thenReturn(List.of(schedule(ScheduleStatus.RESCHEDULED)));

		mockMvc.perform(get(BASE))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].status").value("RESCHEDULED"));
		verify(service).list(PROCESS_ID);
	}
}
