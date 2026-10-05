package com.diegohaefliger.atsresumeoptimizer.ai.application;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
class AiModelCatalog {

	private static final Duration TIMEOUT = Duration.ofSeconds(15);
	private static final String OPENAI_URL = "https://api.openai.com/v1";
	private static final String ANTHROPIC_URL = "https://api.anthropic.com/v1/models?limit=1000";
	private static final String ANTHROPIC_VERSION = "2023-06-01";
	private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models?pageSize=1000";
	private static final String GEMINI_MODEL_PREFIX = "models/";
	private static final String GEMINI_CHAT_METHOD = "generateContent";
	private static final Pattern OPENAI_CHAT_MODEL = Pattern.compile("^(gpt-|o\\d|chatgpt-)");
	private static final Pattern OPENAI_NON_CHAT = Pattern.compile("audio|realtime|tts|transcribe|image|search|embedding|instruct|codex|-pro\\b");

	private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
	private final ObjectMapper objectMapper;

	AiModelCatalog(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	List<String> list(AiProvider provider, String apiKey, String baseUrl) {
		return switch (provider) {
			case OPENAI -> ids(get(OPENAI_URL + "/models", "Authorization", "Bearer " + apiKey).path("data"), "id",
					id -> OPENAI_CHAT_MODEL.matcher(id).find() && !OPENAI_NON_CHAT.matcher(id).find());
			case OPENAI_COMPATIBLE -> ids(get(trimSlash(baseUrl) + "/models", "Authorization", "Bearer " + apiKey).path("data"),
					"id", id -> true);
			case ANTHROPIC -> ids(get(ANTHROPIC_URL, "x-api-key", apiKey, "anthropic-version", ANTHROPIC_VERSION).path("data"),
					"id", id -> true);
			case GEMINI -> geminiModels(get(GEMINI_URL, "x-goog-api-key", apiKey).path("models"));
			case OLLAMA -> ids(get(trimSlash(baseUrl) + "/api/tags").path("models"), "name", id -> true);
		};
	}

	private List<String> geminiModels(JsonNode models) {
		return StreamSupport.stream(models.spliterator(), false)
				.filter(model -> StreamSupport.stream(model.path("supportedGenerationMethods").spliterator(), false)
						.anyMatch(method -> GEMINI_CHAT_METHOD.equals(method.asString())))
				.map(model -> model.path("name").asString().replaceFirst("^" + GEMINI_MODEL_PREFIX, ""))
				.sorted()
				.toList();
	}

	private List<String> ids(JsonNode items, String field, Predicate<String> accepted) {
		return StreamSupport.stream(items.spliterator(), false)
				.map(item -> item.path(field).asString())
				.filter(id -> !id.isBlank())
				.filter(accepted)
				.distinct()
				.sorted()
				.toList();
	}

	private JsonNode get(String url, String... headers) {
		HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(url)).timeout(TIMEOUT).GET();
		if (headers.length > 0) {
			request.headers(headers);
		}
		try {
			HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() / 100 != 2) {
				throw new IllegalStateException("O provedor respondeu HTTP " + response.statusCode() + " ao listar modelos");
			}
			return objectMapper.readTree(response.body());
		} catch (IOException exception) {
			throw new IllegalStateException("Não deu pra falar com o provedor: " + exception.getMessage(), exception);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Listagem de modelos interrompida", exception);
		}
	}

	private static String trimSlash(String url) {
		return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
	}
}
