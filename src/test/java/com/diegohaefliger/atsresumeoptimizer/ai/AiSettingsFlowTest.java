package com.diegohaefliger.atsresumeoptimizer.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest(properties = "app.ai.settings-secret=segredo-de-teste")
@Import(TestcontainersConfiguration.class)
class AiSettingsFlowTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void storesTheKeysEncryptedAndTheProviderPriorityOrder() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

		mockMvc.perform(put("/api/v1/ai-settings")
						.contentType("application/json")
						.content("""
								{"temperature":0.2,"maxOutputTokens":3000,"timeoutSeconds":90,"providers":[
								 {"provider":"GEMINI","enabled":true,"apiKey":"AIza-chave-de-teste-7788",
								  "model":"gemini-2.5-flash","heavyModel":"gemini-2.5-pro"},
								 {"provider":"OLLAMA","enabled":true,"baseUrl":"http://localhost:11434","model":"llama3.1:8b"},
								 {"provider":"ANTHROPIC","enabled":false}]}
								"""))
				.andExpect(status().isOk());

		mockMvc.perform(get("/api/v1/ai-settings"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.providers[0].provider").value("GEMINI"))
				.andExpect(jsonPath("$.providers[1].provider").value("OLLAMA"))
				.andExpect(jsonPath("$.providers[1].priority").value(2))
				.andExpect(jsonPath("$.providers[2].enabled").value(false))
				.andExpect(jsonPath("$.maxOutputTokens").value(3000))
				.andExpect(jsonPath("$.providers[?(@.provider == 'GEMINI')].apiKeySource").value("DATABASE"))
				.andExpect(jsonPath("$.providers[?(@.provider == 'GEMINI')].apiKeyHint").value("••••7788"))
				.andExpect(jsonPath("$.providers[?(@.provider == 'GEMINI')].heavyModel").value("gemini-2.5-pro"));

		String stored = jdbcTemplate.queryForObject(
				"select api_key_encrypted from ai_provider_config where provider = 'GEMINI'", String.class);
		assertThat(stored).isNotBlank().doesNotContain("AIza-chave-de-teste-7788");
	}
}
