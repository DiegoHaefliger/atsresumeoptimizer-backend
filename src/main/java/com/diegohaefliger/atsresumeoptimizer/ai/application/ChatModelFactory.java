package com.diegohaefliger.atsresumeoptimizer.ai.application;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
class ChatModelFactory {

	// Opus/Sonnet 5.5 pensam sempre; o raciocínio consome o limite de saída antes do JSON.
	private static final int ANTHROPIC_MIN_OUTPUT_TOKENS = 16_000;
	private static final Pattern REASONING_MODEL = Pattern.compile("^(o\\d|gpt-5)");

	ChatModel create(AiRuntimeSettings settings, String modelName, int maxOutputTokens) {
		return switch (settings.provider()) {
			case OPENAI, OPENAI_COMPATIBLE -> OpenAiChatModel.builder()
					.apiKey(settings.apiKey())
					.baseUrl(settings.baseUrl())
					.modelName(modelName)
					.temperature(acceptsTemperature(modelName) ? settings.temperature() : null)
					.maxTokens(maxOutputTokens)
					.timeout(settings.timeout())
					.build();
			// Claude 5.5 rejeita temperature diferente do padrão (HTTP 400), então não é enviada.
			case ANTHROPIC -> AnthropicChatModel.builder()
					.apiKey(settings.apiKey())
					.modelName(modelName)
					.maxTokens(Math.max(maxOutputTokens, ANTHROPIC_MIN_OUTPUT_TOKENS))
					.timeout(settings.timeout())
					.build();
			case GEMINI -> GoogleAiGeminiChatModel.builder()
					.apiKey(settings.apiKey())
					.modelName(modelName)
					.temperature(settings.temperature())
					.maxOutputTokens(maxOutputTokens)
					.timeout(settings.timeout())
					.build();
			case OLLAMA -> OllamaChatModel.builder()
					.baseUrl(settings.baseUrl())
					.modelName(modelName)
					.temperature(settings.temperature())
					.numPredict(maxOutputTokens)
					.timeout(settings.timeout())
					.build();
		};
	}

	// Modelos de raciocínio da OpenAI (o1/o3/o4, gpt-5) só aceitam a temperatura padrão.
	private static boolean acceptsTemperature(String modelName) {
		return !REASONING_MODEL.matcher(modelName).find();
	}
}
