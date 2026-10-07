package com.diegohaefliger.atsresumeoptimizer.notification.adapter.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.notification.application.NotificationService;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.Notification;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(NotificationController.class)
@Import(NotificationWebMapperImpl.class)
class NotificationControllerTest {

	private static final UUID ID = UUID.randomUUID();
	private static final Instant NOW = Instant.parse("2026-10-06T12:00:00Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private NotificationService service;

	private Notification notification(Instant readAt) {
		return new Notification(ID, NotificationType.SCHEDULE_REMINDER, "Lembrete: Triagem", "Acme", UUID.randomUUID(),
				UUID.randomUUID(), NOW, readAt);
	}

	@Test
	void listsWithTheDefaultsAndFilters() throws Exception {
		when(service.list(false, 50)).thenReturn(List.of(notification(null)));
		when(service.list(true, 10)).thenReturn(List.of());

		mockMvc.perform(get("/api/v1/notifications"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].title").value("Lembrete: Triagem"))
				.andExpect(jsonPath("$[0].readAt").doesNotExist());
		mockMvc.perform(get("/api/v1/notifications").param("unreadOnly", "true").param("limit", "10"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isEmpty());
	}

	@Test
	void rejectsAnAbsurdLimit() throws Exception {
		mockMvc.perform(get("/api/v1/notifications").param("limit", "100000")).andExpect(status().isBadRequest());

		verifyNoInteractions(service);
	}

	@Test
	void reportsTheUnreadCount() throws Exception {
		when(service.unreadCount()).thenReturn(7L);

		mockMvc.perform(get("/api/v1/notifications/unread-count"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.count").value(7));
	}

	@Test
	void marksOneAndAllAsRead() throws Exception {
		when(service.markRead(ID)).thenReturn(notification(NOW));

		mockMvc.perform(post("/api/v1/notifications/" + ID + "/read"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.readAt").value("2026-10-06T12:00:00Z"));
		mockMvc.perform(post("/api/v1/notifications/read-all")).andExpect(status().isNoContent());
		verify(service).markAllRead();
	}

	@Test
	void deletesAndAnswersNotFound() throws Exception {
		mockMvc.perform(delete("/api/v1/notifications/" + ID)).andExpect(status().isNoContent());
		verify(service).delete(ID);

		when(service.markRead(ID)).thenThrow(new NotificationNotFoundException(ID));
		mockMvc.perform(post("/api/v1/notifications/" + ID + "/read")).andExpect(status().isNotFound());
	}
}
