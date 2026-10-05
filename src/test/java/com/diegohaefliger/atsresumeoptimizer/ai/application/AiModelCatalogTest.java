package com.diegohaefliger.atsresumeoptimizer.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AiModelCatalogTest {

	private HttpServer server;
	private String baseUrl;
	private final AiModelCatalog catalog = new AiModelCatalog(new ObjectMapper());

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		respond("/v1/models", 200, """
				{"data":[{"id":"llama-3.3-70b"},{"id":"deepseek-chat"},{"id":"llama-3.3-70b"}]}""");
		respond("/api/tags", 200, """
				{"models":[{"name":"qwen2.5:14b"},{"name":"llama3.1:8b"}]}""");
		respond("/broken/models", 401, "{}");
		server.start();
		baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@AfterEach
	void stopServer() {
		server.stop(0);
	}

	@Test
	void listsTheModelsOfAnOpenAiCompatibleServerSortedAndWithoutDuplicates() {
		assertThat(catalog.list(AiProvider.OPENAI_COMPATIBLE, "key", baseUrl + "/v1/"))
				.containsExactly("deepseek-chat", "llama-3.3-70b");
	}

	@Test
	void listsTheModelsInstalledInOllama() {
		assertThat(catalog.list(AiProvider.OLLAMA, null, baseUrl)).containsExactly("llama3.1:8b", "qwen2.5:14b");
	}

	@Test
	void failsWithTheHttpStatusWhenTheProviderRefuses() {
		assertThatThrownBy(() -> catalog.list(AiProvider.OPENAI_COMPATIBLE, "key", baseUrl + "/broken"))
				.hasMessageContaining("401");
	}

	private void respond(String path, int status, String body) {
		server.createContext(path, exchange -> {
			byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(status, bytes.length);
			exchange.getResponseBody().write(bytes);
			exchange.close();
		});
	}
}
