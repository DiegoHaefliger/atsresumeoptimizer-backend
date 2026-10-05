package com.diegohaefliger.atsresumeoptimizer.ai.application;

import static com.diegohaefliger.atsresumeoptimizer.ai.application.ProviderCredentials.baseUrlOf;
import static com.diegohaefliger.atsresumeoptimizer.ai.application.ProviderCredentials.blankToNull;

import com.diegohaefliger.atsresumeoptimizer.ai.domain.InvalidAiSettingsException;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
class AiConnectionTester {

	private static final Logger LOGGER = LoggerFactory.getLogger(AiConnectionTester.class);
	private static final String CONNECTION_TEST_PROMPT = "Responda apenas com a palavra OK.";

	private final ProviderCredentials credentials;
	private final ChatModelFactory chatModelFactory;
	private final AiModelCatalog modelCatalog;

	AiConnectionTester(ProviderCredentials credentials, ChatModelFactory chatModelFactory, AiModelCatalog modelCatalog) {
		this.credentials = credentials;
		this.chatModelFactory = chatModelFactory;
		this.modelCatalog = modelCatalog;
	}

	AiConnectionTestResult test(AiProviderTestCommand command) {
		if (!StringUtils.hasText(command.model())) {
			throw new InvalidAiSettingsException("Informe o modelo.");
		}
		String baseUrl = baseUrlOf(command.provider(), command.baseUrl());
		if (command.provider().requiresBaseUrl() && baseUrl == null) {
			throw new InvalidAiSettingsException("Informe a URL base do provedor " + command.provider().label() + ".");
		}
		AiRuntimeSettings settings = new AiRuntimeSettings(command.provider(),
				credentials.resolveApiKey(command.provider(), command.apiKey(), command.baseUrl()), baseUrl,
				command.model().strip(), blankToNull(command.heavyModel()), command.temperature().doubleValue(),
				command.maxOutputTokens(), Duration.ofSeconds(command.timeoutSeconds()));
		long start = System.nanoTime();
		try {
			chatModelFactory.create(settings, settings.model(), settings.maxOutputTokens())
					.chat(ChatRequest.builder().messages(UserMessage.from(CONNECTION_TEST_PROMPT)).build());
			return new AiConnectionTestResult(true, settings.provider(), settings.model(), elapsedMillis(start),
					"Conexão funcionando.");
		} catch (RuntimeException exception) {
			LOGGER.warn("Teste de conexão com {} ({}) falhou", settings.provider(), settings.model(), exception);
			return new AiConnectionTestResult(false, settings.provider(), settings.model(), elapsedMillis(start),
					Optional.ofNullable(exception.getMessage()).orElse(exception.getClass().getSimpleName()));
		}
	}

	AiModelList listModels(AiModelQuery query) {
		String baseUrl = baseUrlOf(query.provider(), query.baseUrl());
		if (query.provider().requiresBaseUrl() && baseUrl == null) {
			return suggested(query.provider(), "Informe a URL base pra listar os modelos do provedor.");
		}
		String apiKey = credentials.resolveApiKey(query.provider(), query.apiKey(), query.baseUrl());
		if (query.provider().requiresApiKey() && !StringUtils.hasText(apiKey)) {
			return suggested(query.provider(), "Informe a chave de API pra listar todos os modelos da sua conta.");
		}
		try {
			List<String> models = modelCatalog.list(query.provider(), apiKey, baseUrl);
			return models.isEmpty()
					? suggested(query.provider(), "O provedor não devolveu nenhum modelo de chat.")
					: new AiModelList(query.provider(), AiModelListSource.PROVIDER, models, null);
		} catch (RuntimeException exception) {
			LOGGER.warn("Listagem de modelos de {} falhou", query.provider(), exception);
			return suggested(query.provider(), exception.getMessage());
		}
	}

	private AiModelList suggested(AiProvider provider, String message) {
		return new AiModelList(provider, AiModelListSource.SUGGESTED, provider.suggestedModels(), message);
	}

	private static long elapsedMillis(long startNanos) {
		return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
	}
}
