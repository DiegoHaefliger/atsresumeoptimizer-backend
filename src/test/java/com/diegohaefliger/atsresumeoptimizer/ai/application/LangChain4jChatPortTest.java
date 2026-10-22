package com.diegohaefliger.atsresumeoptimizer.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.ai.AiCallException;
import com.diegohaefliger.atsresumeoptimizer.ai.ChatBrief;
import com.diegohaefliger.atsresumeoptimizer.ai.ChatReply;
import com.diegohaefliger.atsresumeoptimizer.ai.ChatStreamListener;
import com.diegohaefliger.atsresumeoptimizer.ai.ChatTool;
import com.diegohaefliger.atsresumeoptimizer.ai.ChatTurn;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.TokenUsage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;

class LangChain4jChatPortTest {

	private static final ChatBrief BRIEF =
			new ChatBrief("Vaga: Dev Java", List.of(new ChatTurn(ChatTurn.Speaker.USER, "oi")));

	private final AiModelGateway gateway = mock(AiModelGateway.class);
	private final PromptTemplateRepository templates = mock(PromptTemplateRepository.class);
	private final LangChain4jChatPort port = new LangChain4jChatPort(gateway, templates, new tools.jackson.databind.ObjectMapper());
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

	private static ChatTool tool(String name, java.util.function.Function<Map<String, String>, String> run) {
		return new ChatTool() {
			@Override
			public String name() {
				return name;
			}

			@Override
			public String description() {
				return "consulta";
			}

			@Override
			public List<Parameter> parameters() {
				return List.of(new Parameter("codigo", "código da vaga"));
			}

			@Override
			public String label(Map<String, String> arguments) {
				return "Lendo a vaga #" + arguments.get("codigo");
			}

			@Override
			public String run(Map<String, String> arguments) {
				return run.apply(arguments);
			}
		};
	}

	private static StreamingChatModel asksForTheToolThenAnswers(String toolName, String arguments) {
		int[] calls = {0};
		return new StreamingChatModel() {
			@Override
			public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
				calls[0]++;
				if (calls[0] == 1) {
					handler.onCompleteResponse(ChatResponse.builder()
							.aiMessage(AiMessage.from(ToolExecutionRequest.builder().id("t1").name(toolName).arguments(arguments).build()))
							.tokenUsage(new TokenUsage(20, 3))
							.build());
					return;
				}
				handler.onPartialResponse("A vaga pede Java.");
				handler.onCompleteResponse(ChatResponse.builder()
						.aiMessage(AiMessage.from("A vaga pede Java."))
						.tokenUsage(new TokenUsage(30, 6))
						.build());
			}
		};
	}

	private static final class Recorder implements ChatStreamListener {

		private final List<String> tokens = new ArrayList<>();
		private final List<String> toolLabels = new ArrayList<>();
		private boolean toolsUnavailable;

		@Override
		public boolean onToken(String token) {
			tokens.add(token);
			return true;
		}

		@Override
		public void onToolUse(String label) {
			toolLabels.add(label);
		}

		@Override
		public void onToolsUnavailable() {
			toolsUnavailable = true;
		}
	}

	@Test
	void runsTheRequestedToolAndFeedsTheResultBackToTheModel() {
		givenChain(resolved(AiProvider.OPENAI, asksForTheToolThenAnswers("get_job", "{\"codigo\":\"12\"}")));
		Recorder recorder = new Recorder();
		ChatTool getJob = tool("get_job", arguments -> "Vaga 12: Java, Spring");

		ChatReply reply = port.stream(new ChatBrief("", BRIEF.history(), List.of(getJob)), recorder);

		assertThat(recorder.toolLabels).containsExactly("Lendo a vaga #12");
		assertThat(recorder.tokens).containsExactly("A vaga pede Java.");
		assertThat(reply.usage().tokensIn()).isEqualTo(50);
		assertThat(reply.usage().tokensOut()).isEqualTo(9);
	}

	@Test
	void tellsTheModelWhenAToolFailsInsteadOfBreakingTheChat() {
		List<ChatRequest> seen = new ArrayList<>();
		StreamingChatModel recording = asksForTheToolThenAnswers("get_job", "{\"codigo\":\"12\"}");
		givenChain(resolved(AiProvider.OPENAI, new StreamingChatModel() {
			@Override
			public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
				seen.add(request);
				recording.chat(request, handler);
			}
		}));
		ChatTool broken = tool("get_job", arguments -> {
			throw new IllegalStateException("banco caiu");
		});

		ChatReply reply = port.stream(new ChatBrief("", BRIEF.history(), List.of(broken)), new Recorder());

		assertThat(reply.text()).isEqualTo("A vaga pede Java.");
		ToolExecutionResultMessage result = (ToolExecutionResultMessage) seen.get(1).messages().getLast();
		assertThat(result.text()).isEqualTo("Não consegui consultar isso agora.");
	}

	@Test
	void answersUnknownToolRequestsWithoutRunningAnything() {
		List<ChatRequest> seen = new ArrayList<>();
		StreamingChatModel recording = asksForTheToolThenAnswers("apagar_tudo", "{}");
		givenChain(resolved(AiProvider.OPENAI, new StreamingChatModel() {
			@Override
			public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
				seen.add(request);
				recording.chat(request, handler);
			}
		}));

		port.stream(new ChatBrief("", BRIEF.history(), List.of(tool("get_job", arguments -> "x"))), new Recorder());

		assertThat(((ToolExecutionResultMessage) seen.get(1).messages().getLast()).text()).contains("Ferramenta desconhecida");
	}

	@Test
	void stopsOfferingToolsAfterTooManyRounds() {
		List<ChatRequest> seen = new ArrayList<>();
		givenChain(resolved(AiProvider.OPENAI, new StreamingChatModel() {
			@Override
			public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
				seen.add(request);
				if (request.toolSpecifications() == null || request.toolSpecifications().isEmpty()) {
					handler.onPartialResponse("fim");
					handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from("fim")).build());
					return;
				}
				handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from(
						ToolExecutionRequest.builder().id("t").name("get_job").arguments("{\"codigo\":\"1\"}").build())).build());
			}
		}));

		ChatReply reply = port.stream(new ChatBrief("", BRIEF.history(), List.of(tool("get_job", arguments -> "ok"))),
				new Recorder());

		assertThat(reply.text()).isEqualTo("fim");
		assertThat(seen).hasSize(LangChain4jChatPort.MAX_TOOL_ROUNDS + 1);
	}

	@Test
	void retriesWithoutToolsWhenTheModelRefusesThem() {
		givenChain(resolved(AiProvider.OLLAMA, new StreamingChatModel() {
			@Override
			public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
				if (request.toolSpecifications() != null && !request.toolSpecifications().isEmpty()) {
					handler.onError(new IllegalStateException("registry.ollama.ai/llama3.2 does not support tools"));
					return;
				}
				handler.onPartialResponse("sem ferramentas");
				handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from("sem ferramentas")).build());
			}
		}));
		Recorder recorder = new Recorder();

		ChatReply reply = port.stream(new ChatBrief("", BRIEF.history(), List.of(tool("get_job", arguments -> "ok"))), recorder);

		assertThat(reply.text()).isEqualTo("sem ferramentas");
		assertThat(recorder.toolsUnavailable).isTrue();
	}

	private static StreamingChatModel textOnlyModel(List<ChatRequest> seen) {
		return new StreamingChatModel() {
			@Override
			public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
				seen.add(request);
				boolean hasImage = request.messages().stream().anyMatch(message -> message instanceof dev.langchain4j.data.message.UserMessage user
						&& user.contents().stream().anyMatch(content -> content instanceof dev.langchain4j.data.message.ImageContent));
				if (hasImage) {
					handler.onError(new IllegalStateException("model does not support image input"));
					return;
				}
				handler.onPartialResponse("só texto");
				handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from("só texto")).build());
			}
		};
	}

	@Test
	void sendsTheImagesToTheModelWhenItSeesThem() {
		List<ChatRequest> seen = new ArrayList<>();
		givenChain(resolved(AiProvider.OPENAI, new StreamingChatModel() {
			@Override
			public void chat(ChatRequest request, StreamingChatResponseHandler handler) {
				seen.add(request);
				handler.onPartialResponse("vi");
				handler.onCompleteResponse(ChatResponse.builder().aiMessage(AiMessage.from("vi")).build());
			}
		}));
		ChatBrief brief = new ChatBrief("", List.of(new ChatTurn(ChatTurn.Speaker.USER, "veja",
				List.of(new com.diegohaefliger.atsresumeoptimizer.ai.ChatImage("image/png", "AAAA")))));

		port.stream(brief, token -> true);

		var user = (dev.langchain4j.data.message.UserMessage) seen.getFirst().messages().getLast();
		assertThat(user.contents()).hasSize(2);
		assertThat(user.contents().getLast()).isInstanceOf(dev.langchain4j.data.message.ImageContent.class);
	}

	@Test
	void retriesWithoutImagesWhenTheModelCannotSeeThem() {
		List<ChatRequest> seen = new ArrayList<>();
		givenChain(resolved(AiProvider.OLLAMA, textOnlyModel(seen)));
		ChatBrief brief = new ChatBrief("", List.of(new ChatTurn(ChatTurn.Speaker.USER, "veja",
				List.of(new com.diegohaefliger.atsresumeoptimizer.ai.ChatImage("image/png", "AAAA")))));
		boolean[] imagesUnavailable = {false};

		ChatReply reply = port.stream(brief, new ChatStreamListener() {
			@Override
			public boolean onToken(String token) {
				return true;
			}

			@Override
			public void onImagesUnavailable() {
				imagesUnavailable[0] = true;
			}
		});

		assertThat(reply.text()).isEqualTo("só texto");
		assertThat(imagesUnavailable[0]).isTrue();
		assertThat(seen).hasSize(2);
	}
}
