package com.diegohaefliger.atsresumeoptimizer.diagnostics.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.diagnostics.DiagnosticEntry;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.DiagnosticSection;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.application.DiagnosticsService;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.domain.DiagnosticBundle;
import com.diegohaefliger.atsresumeoptimizer.diagnostics.domain.DiagnosticReport;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DiagnosticsController.class)
@Import(DiagnosticsWebMapperImpl.class)
class DiagnosticsControllerTest {

	private static final String ID_PATTERN = "[2-9A-HJ-NP-Z]{8}";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private DiagnosticsService service;

	@Test
	void returnsTheReportWithACopyableText() throws Exception {
		when(service.report()).thenReturn(new DiagnosticReport(Instant.parse("2026-11-25T13:30:00Z"),
				List.of(new DiagnosticSection("Sistema", List.of(new DiagnosticEntry("Java", "25")))), true, false));

		mockMvc.perform(get("/api/v1/diagnostics"))
				.andExpect(status().isOk())
				.andExpect(header().string("X-Request-Id", org.hamcrest.Matchers.matchesPattern(ID_PATTERN)))
				.andExpect(jsonPath("$.sections[0].entries[0].value").value("25"))
				.andExpect(jsonPath("$.sections[0].entries[0].personal").value(false))
				.andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("- Java: 25")))
				.andExpect(jsonPath("$.logFileAvailable").value(true));
	}

	@Test
	void downloadsTheBundleAsAZipAttachment() throws Exception {
		when(service.bundle()).thenReturn(new DiagnosticBundle("diagnostico-ats-20261125-1030.zip", new byte[] {1, 2}));

		mockMvc.perform(get("/api/v1/diagnostics/bundle"))
				.andExpect(status().isOk())
				.andExpect(header().string("Content-Type", "application/zip"))
				.andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("diagnostico-ats-20261125-1030.zip")));
	}

	@Test
	void recordsAClientError() throws Exception {
		when(service.recordClientError(any())).thenReturn("K7Q3XM2A");

		mockMvc.perform(post("/api/v1/diagnostics/client-errors").contentType(MediaType.APPLICATION_JSON)
						.content("{\"message\":\"x is undefined\",\"page\":\"/jobs\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.errorId").value("K7Q3XM2A"));
	}

	@Test
	void invalidInputStillCarriesAnErrorId() throws Exception {
		mockMvc.perform(post("/api/v1/diagnostics/client-errors").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errorId").value(org.hamcrest.Matchers.matchesPattern(ID_PATTERN)));
	}

	@Test
	void anUnexpectedFailureAnswersAFriendlyMessageAndTheErrorId() throws Exception {
		when(service.report()).thenThrow(new NullPointerException("campo nulo"));

		mockMvc.perform(get("/api/v1/diagnostics"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("código do erro")))
				.andExpect(jsonPath("$.errorId").value(org.hamcrest.Matchers.matchesPattern(ID_PATTERN)));
	}

	@Test
	void anUnknownRouteStaysA404() throws Exception {
		mockMvc.perform(get("/api/v1/diagnostics/nao-existe")).andExpect(status().isNotFound());
	}
}
