package com.diegohaefliger.atsresumeoptimizer.release.adapter.web;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.release.application.UpdateCheckService;
import com.diegohaefliger.atsresumeoptimizer.release.domain.AppVersion;
import com.diegohaefliger.atsresumeoptimizer.release.domain.UpdateStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AppVersionControllerTest {

	private final UpdateCheckService service = mock(UpdateCheckService.class);
	private final MockMvc mvc = MockMvcBuilders
			.standaloneSetup(new AppVersionController(service, new AppVersionWebMapperImpl()))
			.build();

	@Test
	void returnsTheInstalledAndTheLatestVersion() throws Exception {
		when(service.status()).thenReturn(new UpdateStatus(new AppVersion(1, 0, 0, null), new AppVersion(1, 1, 0, null),
				true, "https://github.com/DiegoHaefliger/atsresumeoptimizer/releases/tag/v1.1.0",
				Instant.parse("2026-10-10T12:00:00Z")));

		mvc.perform(get("/api/v1/app/version"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.current").value("1.0.0"))
				.andExpect(jsonPath("$.latest").value("1.1.0"))
				.andExpect(jsonPath("$.updateAvailable").value(true))
				.andExpect(jsonPath("$.releaseUrl").value("https://github.com/DiegoHaefliger/atsresumeoptimizer/releases/tag/v1.1.0"));
	}

	@Test
	void returnsOnlyTheInstalledVersionWhenNothingIsKnownAboutReleases() throws Exception {
		when(service.status()).thenReturn(new UpdateStatus(new AppVersion(1, 0, 0, null), null, false, null, null));

		mvc.perform(get("/api/v1/app/version"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.current").value("1.0.0"))
				.andExpect(jsonPath("$.updateAvailable").value(false))
				.andExpect(jsonPath("$.latest").doesNotExist());
	}
}
