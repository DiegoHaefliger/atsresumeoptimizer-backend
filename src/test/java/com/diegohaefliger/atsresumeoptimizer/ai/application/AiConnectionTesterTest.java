package com.diegohaefliger.atsresumeoptimizer.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiConnectionTesterTest {

	@Mock
	private AiSettingsRepository settingsRepository;
	@Mock
	private AiProviderConfigRepository providerConfigRepository;
	@Mock
	private ChatModelFactory chatModelFactory;
	@Mock
	private AiModelCatalog modelCatalog;

	private AiConnectionTester tester;

	@BeforeEach
	void setUp() {
		AiProperties properties = ApiKeyCipherTest.properties("segredo");
		ApiKeyCipher cipher = new ApiKeyCipher(new SettingsSecretStore(properties));
		tester = new AiConnectionTester(new ProviderCredentials(providerConfigRepository, cipher),
				chatModelFactory, modelCatalog);
		lenient().when(providerConfigRepository.findById(any())).thenReturn(Optional.empty());
	}

	@Test
	void reportsAFailedConnectionTestWithoutThrowing() {
		ChatModel failing = mock(ChatModel.class);
		when(failing.chat(any(ChatRequest.class))).thenThrow(new RuntimeException("401 invalid api key"));
		when(chatModelFactory.create(any(), anyString(), anyInt())).thenReturn(failing);

		AiConnectionTestResult result = tester.test(new AiProviderTestCommand(AiProvider.OPENAI, "sk-errada", null,
				"gpt-4o-mini", null, new BigDecimal("0.10"), 2000, 120));

		assertThat(result.success()).isFalse();
		assertThat(result.message()).contains("401");
	}

	@Test
	void listsTheModelsOfTheAccountUsingTheTypedKey() {
		when(modelCatalog.list(AiProvider.OPENAI, "sk-digitada", null)).thenReturn(List.of("gpt-4o", "o3"));

		AiModelList models = tester.listModels(new AiModelQuery(AiProvider.OPENAI, "sk-digitada", null));

		assertThat(models.source()).isEqualTo(AiModelListSource.PROVIDER);
		assertThat(models.models()).containsExactly("gpt-4o", "o3");
	}

	@Test
	void fallsBackToTheSuggestedModelsWithoutAKeyOrWhenTheProviderFails() {
		AiModelList withoutKey = tester.listModels(new AiModelQuery(AiProvider.ANTHROPIC, null, null));
		when(modelCatalog.list(AiProvider.GEMINI, "AIza-x", null)).thenThrow(new IllegalStateException("HTTP 403"));
		AiModelList failed = tester.listModels(new AiModelQuery(AiProvider.GEMINI, "AIza-x", null));

		assertThat(withoutKey.source()).isEqualTo(AiModelListSource.SUGGESTED);
		assertThat(withoutKey.models()).contains("claude-opus-5-5", "claude-haiku-4-5");
		assertThat(failed.message()).contains("403");
		verify(modelCatalog, never()).list(eq(AiProvider.ANTHROPIC), any(), any());
	}
}
