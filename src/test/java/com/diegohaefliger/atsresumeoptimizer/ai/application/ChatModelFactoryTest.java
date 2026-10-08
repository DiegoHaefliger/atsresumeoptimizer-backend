package com.diegohaefliger.atsresumeoptimizer.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import dev.langchain4j.model.chat.ChatModel;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChatModelFactoryTest {

	private static final String COMPLETION = """
			{"id":"c","object":"chat.completion","created":1,"model":"m",
			 "choices":[{"index":0,"message":{"role":"assistant","content":"ok"},"finish_reason":"stop"}],
			 "usage":{"prompt_tokens":1,"completion_tokens":1,"total_tokens":2}}""";

	private final ChatModelFactory factory = new ChatModelFactory();
	private final AtomicReference<String> requestBody = new AtomicReference<>();
	private HttpServer server;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			byte[] response = COMPLETION.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, response.length);
			exchange.getResponseBody().write(response);
			exchange.close();
		});
		server.start();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	@Test
	void sendsMaxCompletionTokensInsteadOfMaxTokensToOpenAiReasoningModels() {
		for (String model : new String[] {"o3-mini", "o4-mini", "gpt-5", "gpt-5-mini"}) {
			call(model, 1000);

			assertThat(body()).as(model).containsEntry("max_completion_tokens", 1000).doesNotContainKeys("max_tokens", "temperature");
		}
	}

	@Test
	void keepsSendingMaxTokensAndTemperatureToOtherOpenAiModels() {
		call("gpt-4o", 1000);

		assertThat(body()).containsEntry("max_tokens", 1000).containsEntry("temperature", 0.1)
				.doesNotContainKey("max_completion_tokens");
	}

	private Map<String, Object> body() {
		try {
			return new ObjectMapper().readValue(requestBody.get(), new TypeReference<>() {
			});
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void call(String model, int maxOutputTokens) {
		AiRuntimeSettings settings = new AiRuntimeSettings(AiProvider.OPENAI, "k",
				"http://127.0.0.1:" + server.getAddress().getPort(), model, null, 0.1, maxOutputTokens,
				Duration.ofSeconds(10));
		ChatModel chatModel = factory.create(settings, model, maxOutputTokens);
		chatModel.chat("oi");
	}
}
