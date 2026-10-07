package com.diegohaefliger.atsresumeoptimizer.notification.adapter.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.notification.application.NotificationSettingsService;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.InvalidNotificationSettingsException;
import com.diegohaefliger.atsresumeoptimizer.notification.domain.NotificationSettings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;

@WebMvcTest(NotificationSettingsController.class)
@Import(NotificationSettingsWebMapperImpl.class)
class NotificationSettingsControllerTest {

	private static final String URL = "/api/v1/notification-settings";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private NotificationSettingsService service;

	@Test
	void returnsTheSettingsWithTheChannelsTheAppKnows() throws Exception {
		when(service.current()).thenReturn(NotificationSettings.DEFAULT);

		mockMvc.perform(get(URL))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.leadMinutes[0]").value(60))
				.andExpect(jsonPath("$.leadMinutes[1]").value(1440))
				.andExpect(jsonPath("$.timezone").value("America/Sao_Paulo"))
				.andExpect(jsonPath("$.channels[0]").value("IN_APP"))
				.andExpect(jsonPath("$.availableChannels[?(@.channel=='IN_APP')].available").value(true))
				.andExpect(jsonPath("$.availableChannels[?(@.channel=='EMAIL')].available").value(false));
	}

	@Test
	void savesValidSettings() throws Exception {
		when(service.save(any())).thenReturn(NotificationSettings.DEFAULT);

		mockMvc.perform(put(URL)
						.contentType("application/json")
						.content("{\"leadMinutes\":[60,1440],\"channels\":[\"IN_APP\"],\"timezone\":\"America/Sao_Paulo\"}"))
				.andExpect(status().isOk());
	}

	@Test
	void rejectsMalformedSettings() throws Exception {
		mockMvc.perform(put(URL).contentType("application/json").content("{\"leadMinutes\":[],\"channels\":[\"IN_APP\"],\"timezone\":\"UTC\"}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(put(URL).contentType("application/json").content("{\"leadMinutes\":[-5],\"channels\":[\"IN_APP\"],\"timezone\":\"UTC\"}"))
				.andExpect(status().isBadRequest());
		mockMvc.perform(put(URL).contentType("application/json").content("{\"leadMinutes\":[60],\"channels\":[\"IN_APP\"],\"timezone\":\"Marte/Olympus\"}"))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(service);
	}

	@Test
	void answersBadRequestWhenTheServiceRefuses() throws Exception {
		when(service.save(any())).thenThrow(new InvalidNotificationSettingsException("Canal ainda indisponível: EMAIL."));

		mockMvc.perform(put(URL).contentType("application/json").content("{\"leadMinutes\":[60],\"channels\":[\"EMAIL\"],\"timezone\":\"UTC\"}"))
				.andExpect(status().isBadRequest());
	}
}
