package com.diegohaefliger.atsresumeoptimizer.ai.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class StreamingChatModelFactoryTest {

	private static final String STREAM = """
			data: {"id":"c","object":"chat.completion.chunk","created":1,"model":"m","choices":[{"index":0,"delta":{"role":"assistant","content":"Olá"},"finish_reason":null}]}

			data: {"id":"c","object":"chat.completion.chunk","created":1,"model":"m","choices":[{"index":0,"delta":{"content":" mundo"},"finish_reason":null}]}

			data: {"id":"c","object":"chat.completion.chunk","created":1,"model":"m","choices":[{"index":0,"delta":{},"finish_reason":"stop"}]}

			data: [DONE]

			""";

	private final ChatModelFactory factory = new ChatModelFactory();
	private final AtomicReference<String> requestBody = new AtomicReference<>();
	private HttpServer server;

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
			byte[] response = STREAM.getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
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
	void streamsTokensFromAnOpenAiCompatibleEndpointWithTheSameParametersAsTheBlockingModel() throws Exception {
		AiRuntimeSettings settings = new AiRuntimeSettings(AiProvider.OPENAI_COMPATIBLE, "chave",
				"http://127.0.0.1:" + server.getAddress().getPort(), "gpt-4o", "gpt-4o", 0.2, 1000, Duration.ofSeconds(10));
		List<String> tokens = new ArrayList<>();
		CompletableFuture<ChatResponse> finished = new CompletableFuture<>();

		factory.createStreaming(settings, "gpt-4o", 1000).chat(
				ChatRequest.builder().messages(UserMessage.from("oi")).build(), new StreamingChatResponseHandler() {
					@Override
					public void onPartialResponse(String partial) {
						tokens.add(partial);
					}

					@Override
					public void onCompleteResponse(ChatResponse response) {
						finished.complete(response);
					}

					@Override
					public void onError(Throwable error) {
						finished.completeExceptionally(error);
					}
				});
		finished.get(10, TimeUnit.SECONDS);

		assertThat(tokens).containsExactly("Olá", " mundo");
		assertThat(requestBody.get().replaceAll("\\s", "")).contains("\"stream\":true").contains("\"max_tokens\":1000");
	}
}
