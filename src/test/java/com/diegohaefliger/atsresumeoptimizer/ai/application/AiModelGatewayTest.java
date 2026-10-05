package com.diegohaefliger.atsresumeoptimizer.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.langchain4j.model.chat.ChatModel;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiModelGatewayTest {

	@Mock
	private AiRuntimeSettingsProvider settingsProvider;
	@Mock
	private ChatModelFactory factory;

	private final AiProperties properties = new AiProperties(0.1, 2000, Duration.ofSeconds(60),
			Map.of("resume-structuring", new AiProperties.TaskOverride(16000)), "s", null);

	@Test
	void usesTheHeavyModelAndTheTaskTokenLimitForResumeStructuringAndReusesBuiltModels() {
		var gateway = new AiModelGateway(settingsProvider, factory, properties);
		AiRuntimeSettings settings = new AiRuntimeSettings(AiProvider.GEMINI, "k", null, "gemini-2.5-flash",
				"gemini-2.5-pro", 0.1, 3000, Duration.ofSeconds(60));
		when(settingsProvider.chain()).thenReturn(java.util.List.of(settings));
		when(factory.create(any(), any(), org.mockito.ArgumentMatchers.anyInt())).thenAnswer(invocation -> mock(ChatModel.class));

		ResolvedModel heavy = gateway.resolveChain("resume-structuring").getFirst();
		ResolvedModel light = gateway.resolveChain("job-structuring").getFirst();
		gateway.resolveChain("job-structuring");

		assertThat(heavy.modelName()).isEqualTo("gemini-2.5-pro");
		assertThat(light.modelName()).isEqualTo("gemini-2.5-flash");
		verify(factory).create(settings, "gemini-2.5-pro", 16000);
		verify(factory, times(1)).create(eq(settings), eq("gemini-2.5-flash"), eq(3000));
	}
}
