package com.diegohaefliger.atsresumeoptimizer.linkedin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.TestcontainersConfiguration;
import com.diegohaefliger.atsresumeoptimizer.ai.AiPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class LinkedInSnapshotFlowTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	@MockitoBean
	private AiPort aiPort;

	@Test
	void listsTheStoredReadingsWithTheirSourceNewestFirst() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		mockMvc.perform(post("/api/v1/linkedin/snapshots/manual").contentType("application/json")
						.content("{\"profile\":{\"fullName\":\"Ana\",\"headline\":\"Dev Java\"}}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.source").value("TEXT"));

		mockMvc.perform(get("/api/v1/linkedin/snapshots"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").exists())
				.andExpect(jsonPath("$[0].source").value("TEXT"))
				.andExpect(jsonPath("$[0].capturedAt").exists())
				.andExpect(jsonPath("$[0].headline").value("Dev Java"));
	}

	@Test
	void allowsTheAppOriginToListTheReadingsAcrossPorts() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
		mockMvc.perform(post("/api/v1/linkedin/snapshots/manual").contentType("application/json")
				.content("{\"profile\":{\"fullName\":\"Ana\",\"headline\":\"Dev Java\"}}")).andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/linkedin/snapshots").header("Origin", "http://localhost:5173"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
	}

	@Test
	void keepsTheExtensionOriginAllowedToSendReadings() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

		mockMvc.perform(options("/api/v1/linkedin/snapshots")
						.header("Origin", "chrome-extension://abcdefghijklmnop")
						.header("Access-Control-Request-Method", "POST")
						.header("Access-Control-Request-Headers", "content-type,x-pairing-token"))
				.andExpect(status().isOk())
				.andExpect(header().string("Access-Control-Allow-Origin", "chrome-extension://abcdefghijklmnop"));
	}
}
