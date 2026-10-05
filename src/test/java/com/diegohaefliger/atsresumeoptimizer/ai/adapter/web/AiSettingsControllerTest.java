package com.diegohaefliger.atsresumeoptimizer.ai.adapter.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.diegohaefliger.atsresumeoptimizer.ai.application.AiConnectionTestResult;
import com.diegohaefliger.atsresumeoptimizer.ai.application.AiModelList;
import com.diegohaefliger.atsresumeoptimizer.ai.application.AiModelListSource;
import com.diegohaefliger.atsresumeoptimizer.ai.application.AiModelQuery;
import com.diegohaefliger.atsresumeoptimizer.ai.application.AiProvider;
import com.diegohaefliger.atsresumeoptimizer.ai.application.AiProviderTestCommand;
import com.diegohaefliger.atsresumeoptimizer.ai.application.AiProviderView;
import com.diegohaefliger.atsresumeoptimizer.ai.application.AiSettingsService;
import com.diegohaefliger.atsresumeoptimizer.ai.application.AiSettingsUpdate;
import com.diegohaefliger.atsresumeoptimizer.ai.application.AiSettingsView;
import com.diegohaefliger.atsresumeoptimizer.ai.application.ApiKeySource;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AiSettingsController.class)
@Import(AiSettingsRequestMapperImpl.class)
class AiSettingsControllerTest {

	private static final String VALID_BODY = """
			{"temperature":0.1,"maxOutputTokens":2000,"timeoutSeconds":120,"providers":[
			 {"provider":"ANTHROPIC","enabled":true,"apiKey":"sk-ant-1","model":"claude-sonnet-5-5"},
			 {"provider":"OPENAI","enabled":true,"model":"gpt-4o-mini"}]}""";

	private static final String TEST_BODY = """
			{"provider":"ANTHROPIC","apiKey":"sk-ant-1","model":"claude-sonnet-5-5","temperature":0.1,
			 "maxOutputTokens":2000,"timeoutSeconds":120}""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private AiSettingsService settingsService;

	@Test
	void returnsTheSettingsWithTheKeyHintOnly() throws Exception {
		when(settingsService.current()).thenReturn(new AiSettingsView(new BigDecimal("0.10"), 2000, 120,
				true, List.of(new AiProviderView(AiProvider.OPENAI, "OpenAI", true, 1, true, false, null, List.of("gpt-4o-mini"),
						"gpt-4o-mini", "gpt-4o", null, ApiKeySource.DATABASE, "••••1234"))));

		mockMvc.perform(get("/api/v1/ai-settings"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.providers[0].priority").value(1))
				.andExpect(jsonPath("$.providers[0].apiKeyHint").value("••••1234"))
				.andExpect(jsonPath("$.providers[0].apiKey").doesNotExist());
	}

	@Test
	void savesTheSettings() throws Exception {
		when(settingsService.save(any(AiSettingsUpdate.class))).thenReturn(
				new AiSettingsView(new BigDecimal("0.10"), 2000, 120, true, List.of()));

		mockMvc.perform(put("/api/v1/ai-settings").contentType("application/json").content(VALID_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.encryptionAvailable").value(true));

		org.mockito.ArgumentCaptor<AiSettingsUpdate> update = org.mockito.ArgumentCaptor.forClass(AiSettingsUpdate.class);
		org.mockito.Mockito.verify(settingsService).save(update.capture());
		org.assertj.core.api.Assertions.assertThat(update.getValue().providers())
				.extracting(com.diegohaefliger.atsresumeoptimizer.ai.application.AiProviderUpdate::provider)
				.containsExactly(AiProvider.ANTHROPIC, AiProvider.OPENAI);
	}

	@Test
	void runsTheConnectionTest() throws Exception {
		when(settingsService.test(any(AiProviderTestCommand.class))).thenReturn(
				new AiConnectionTestResult(true, AiProvider.ANTHROPIC, "claude-sonnet-5-5", 420, "Conexão funcionando."));

		mockMvc.perform(post("/api/v1/ai-settings/test").contentType("application/json").content(TEST_BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.latencyMs").value(420));
	}

	@Test
	void listsTheModelsOfTheProvider() throws Exception {
		when(settingsService.listModels(any(AiModelQuery.class))).thenReturn(
				new AiModelList(AiProvider.OLLAMA, AiModelListSource.PROVIDER, List.of("llama3.1:8b"), null));

		mockMvc.perform(post("/api/v1/ai-settings/models").contentType("application/json")
						.content("{\"provider\":\"OLLAMA\",\"baseUrl\":\"http://localhost:11434\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.source").value("PROVIDER"))
				.andExpect(jsonPath("$.models[0]").value("llama3.1:8b"));
	}

	@Test
	void rejectsAnOutOfRangeTemperatureAndANonHttpBaseUrl() throws Exception {
		mockMvc.perform(put("/api/v1/ai-settings").contentType("application/json").content("""
						{"temperature":5,"maxOutputTokens":2000,"timeoutSeconds":120,"providers":[
						 {"provider":"OLLAMA","enabled":true,"baseUrl":"file:///etc","model":"llama3.1:8b"}]}"""))
				.andExpect(status().isBadRequest());

		verifyNoInteractions(settingsService);
	}
}
