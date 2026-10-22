package com.diegohaefliger.atsresumeoptimizer.ai.application;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiStreamingChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
class ChatModelFactory {

	// Opus/Sonnet 5.5 pensam sempre; o raciocínio consome o limite de saída antes do JSON.
	private static final int ANTHROPIC_MIN_OUTPUT_TOKENS = 16_000;
	// Modelo que raciocina gasta o limite pensando e devolve texto vazio; o piso deixa folga pro JSON.
	private static final int OPENAI_REASONING_MIN_OUTPUT_TOKENS = 16_000;
	private static final Pattern REASONING_MODEL = Pattern.compile("^(o\\d|gpt-5)");
	// Só a família clássica aceita temperatura; modelo novo ou desconhecido (ex.: gpt-6-*) usa o padrão da OpenAI.
	private static final Pattern CLASSIC_OPENAI_MODEL = Pattern.compile("^(gpt-[34]|chatgpt)");

	ChatModel create(AiRuntimeSettings settings, String modelName, int maxOutputTokens) {
		return switch (settings.provider()) {
			case OPENAI, OPENAI_COMPATIBLE -> {
				OpenAiParams params = openAiParams(settings, modelName, maxOutputTokens);
				yield openAi(settings, modelName)
						.temperature(params.temperature())
						.maxTokens(params.maxTokens())
						.maxCompletionTokens(params.maxCompletionTokens())
						.build();
			}
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

	StreamingChatModel createStreaming(AiRuntimeSettings settings, String modelName, int maxOutputTokens) {
		return switch (settings.provider()) {
			case OPENAI, OPENAI_COMPATIBLE -> {
				OpenAiParams params = openAiParams(settings, modelName, maxOutputTokens);
				yield OpenAiStreamingChatModel.builder()
						.apiKey(settings.apiKey())
						.baseUrl(settings.baseUrl())
						.modelName(modelName)
						.timeout(settings.timeout())
						.temperature(params.temperature())
						.maxTokens(params.maxTokens())
						.maxCompletionTokens(params.maxCompletionTokens())
						.build();
			}
			case ANTHROPIC -> AnthropicStreamingChatModel.builder()
					.apiKey(settings.apiKey())
					.modelName(modelName)
					.maxTokens(Math.max(maxOutputTokens, ANTHROPIC_MIN_OUTPUT_TOKENS))
					.timeout(settings.timeout())
					.build();
			case GEMINI -> GoogleAiGeminiStreamingChatModel.builder()
					.apiKey(settings.apiKey())
					.modelName(modelName)
					.temperature(settings.temperature())
					.maxOutputTokens(maxOutputTokens)
					.timeout(settings.timeout())
					.build();
			case OLLAMA -> OllamaStreamingChatModel.builder()
					.baseUrl(settings.baseUrl())
					.modelName(modelName)
					.temperature(settings.temperature())
					.numPredict(maxOutputTokens)
					.timeout(settings.timeout())
					.build();
		};
	}

	// max_tokens é legado na OpenAI e os modelos novos o rejeitam; max_completion_tokens vale para todos.
	private static OpenAiParams openAiParams(AiRuntimeSettings settings, String modelName, int maxOutputTokens) {
		if (settings.provider() == AiProvider.OPENAI) {
			boolean classic = CLASSIC_OPENAI_MODEL.matcher(modelName).find();
			return new OpenAiParams(classic ? settings.temperature() : null, null,
					classic ? maxOutputTokens : Math.max(maxOutputTokens, OPENAI_REASONING_MIN_OUTPUT_TOKENS));
		}
		boolean reasoning = isReasoning(modelName);
		return new OpenAiParams(reasoning ? null : settings.temperature(), reasoning ? null : maxOutputTokens,
				reasoning ? maxOutputTokens : null);
	}

	private record OpenAiParams(Double temperature, Integer maxTokens, Integer maxCompletionTokens) {
	}

	private static OpenAiChatModel.OpenAiChatModelBuilder openAi(AiRuntimeSettings settings, String modelName) {
		return OpenAiChatModel.builder()
				.apiKey(settings.apiKey())
				.baseUrl(settings.baseUrl())
				.modelName(modelName)
				.timeout(settings.timeout());
	}

	// Endpoints compatíveis (Azure etc.): modelos de raciocínio só aceitam a temperatura padrão e rejeitam max_tokens.
	private static boolean isReasoning(String modelName) {
		return REASONING_MODEL.matcher(modelName).find();
	}
}
