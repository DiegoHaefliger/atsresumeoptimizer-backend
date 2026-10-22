package com.diegohaefliger.atsresumeoptimizer.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.ai.AiCallException;
import com.diegohaefliger.atsresumeoptimizer.ai.ChatBrief;
import com.diegohaefliger.atsresumeoptimizer.ai.ChatReply;
import com.diegohaefliger.atsresumeoptimizer.ai.ChatTurn;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.TokenUsage;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class LangChain4jChatPortTest {

	private static final ChatBrief BRIEF =
			new ChatBrief("Vaga: Dev Java", List.of(new ChatTurn(ChatTurn.Speaker.USER, "oi")));

	private final AiModelGateway gateway = mock(AiModelGateway.class);
	private final PromptTemplateRepository templates = mock(PromptTemplateRepository.class);
	private final LangChain4jChatPort port = new LangChain4jChatPort(gateway, templates);
	private final List<ChatRequest> requests = new ArrayList<>();

	private StreamingChatModel model(Consumer<StreamingChatResponseHandler> script) {
		return new StreamingChatModel() {
			@Override
			public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
				requests.add(request);
				script.accept(handler);
			}
		};
	}

	private ResolvedStreamingModel resolved(AiProvider provider, StreamingChatModel model) {
		return new ResolvedStreamingModel(model, provider, "modelo-" + provider.name().toLowerCase());
	}

	private StreamingChatModel replying(String... tokens) {
		return model(handler -> {
			for (String token : tokens) {
				handler.onPartialResponse(token);
			}
			handler.onCompleteResponse(ChatResponse.builder()
					.aiMessage(AiMessage.from(String.join("", tokens)))
					.tokenUsage(new TokenUsage(10, 5))
					.build());
		});
	}

	private void givenChain(ResolvedStreamingModel... chain) {
		when(templates.findFirstByKeyOrderByVersionDesc("chat-recruiter"))
				.thenReturn(Optional.of(new PromptTemplate(UUID.randomUUID(), "chat-recruiter", 1, "Você é recrutador.", "m")));
		when(gateway.resolveStreamingChain("chat-recruiter")).thenReturn(List.of(chain));
	}

	@Test
	void streamsTheTokensInOrderAndReportsUsage() {
		givenChain(resolved(AiProvider.OPENAI, replying("Olá", ", tudo", " bem?")));
		List<String> received = new ArrayList<>();

		ChatReply reply = port.stream(BRIEF, token -> received.add(token));

		assertThat(received).containsExactly("Olá", ", tudo", " bem?");
		assertThat(reply.text()).isEqualTo("Olá, tudo bem?");
		assertThat(reply.interrupted()).isFalse();
		assertThat(reply.usage().tokensIn()).isEqualTo(10);
		assertThat(reply.usage().tokensOut()).isEqualTo(5);
	}

	@Test
	void sendsThePromptWithTheContextMarkedAsDataAndTheHistory() {
		givenChain(resolved(AiProvider.OPENAI, replying("ok")));

		port.stream(BRIEF, token -> true);

		SystemMessage system = (SystemMessage) requests.getFirst().messages().getFirst();
		assertThat(system.text()).startsWith("Você é recrutador.").contains("ignorada").endsWith("Vaga: Dev Java");
		assertThat(requests.getFirst().messages()).hasSize(2);
	}

	@Test
	void fallsBackToTheNextProviderWhenTheFirstFailsBeforeAnyToken() {
		StreamingChatModel broken = model(handler -> handler.onError(new IllegalStateException("429")));
		givenChain(resolved(AiProvider.OPENAI, broken), resolved(AiProvider.ANTHROPIC, replying("resposta")));

		ChatReply reply = port.stream(BRIEF, token -> true);

		assertThat(reply.text()).isEqualTo("resposta");
		assertThat(reply.usage().model()).isEqualTo("modelo-anthropic");
	}

	@Test
	void doesNotFallBackOnceATokenHasBeenSent() {
		StreamingChatModel dropsMidway = model(handler -> {
			handler.onPartialResponse("meio");
			handler.onError(new IllegalStateException("conexão caiu"));
		});
		givenChain(resolved(AiProvider.OPENAI, dropsMidway), resolved(AiProvider.ANTHROPIC, replying("outra")));
		List<String> received = new ArrayList<>();

		assertThatThrownBy(() -> port.stream(BRIEF, token -> received.add(token)))
				.isInstanceOf(AiCallException.class);
		assertThat(received).containsExactly("meio");
		assertThat(requests).hasSize(1);
	}

	@Test
	void failsWithTheLastErrorWhenEveryProviderFails() {
		StreamingChatModel broken = model(handler -> handler.onError(new IllegalStateException("fora do ar")));
		givenChain(resolved(AiProvider.OPENAI, broken), resolved(AiProvider.ANTHROPIC, broken));

		assertThatThrownBy(() -> port.stream(BRIEF, token -> true))
				.isInstanceOf(AiCallException.class)
				.hasMessageContaining("Anthropic");
	}

	@Test
	void stopsWhenTheListenerAsksAndIgnoresTheRest() {
		givenChain(resolved(AiProvider.OPENAI, replying("um ", "dois ", "três")));
		List<String> received = new ArrayList<>();

		ChatReply reply = port.stream(BRIEF, token -> {
			received.add(token);
			return received.size() < 2;
		});

		assertThat(received).containsExactly("um ", "dois ");
		assertThat(reply.interrupted()).isTrue();
		assertThat(reply.text()).isEqualTo("um dois ");
	}

	@Test
	void explainsWhenNoProviderIsConfigured() {
		givenChain();

		assertThatThrownBy(() -> port.stream(BRIEF, token -> true))
				.isInstanceOf(AiCallException.class)
				.hasMessageContaining("Nenhum provedor de IA configurado");
	}
}
