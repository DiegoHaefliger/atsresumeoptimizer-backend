package com.diegohaefliger.atsresumeoptimizer.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.ai.AiCallException;
import com.diegohaefliger.atsresumeoptimizer.ai.AiUsage;
import com.diegohaefliger.atsresumeoptimizer.ai.ChatBrief;
import com.diegohaefliger.atsresumeoptimizer.ai.ChatPort;
import com.diegohaefliger.atsresumeoptimizer.ai.ChatReply;
import com.diegohaefliger.atsresumeoptimizer.ai.ChatStreamListener;
import com.diegohaefliger.atsresumeoptimizer.ai.ChatTurn;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatMessage;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatRole;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ChatReplyServiceImplTest {

	private static final UUID CONVERSATION = UUID.randomUUID();
	private static final Instant NOW = Instant.parse("2026-10-22T12:00:00Z");
	private static final AiUsage USAGE = new AiUsage("OpenAI", "gpt", 10, 5, new BigDecimal("0.001"), false);

	private final ChatMessageStore store = mock(ChatMessageStore.class);
	private final ChatPort chatPort = mock(ChatPort.class);
	private final ChatReplyServiceImpl service = new ChatReplyServiceImpl(store, chatPort);
	private final Recorder recorder = new Recorder();

	private static ChatMessage message(ChatRole role, String content) {
		return new ChatMessage(UUID.randomUUID(), role, content, null, 0, 0, BigDecimal.ZERO, false, NOW);
	}

	private PendingReply pending(String question, List<ChatMessage> history) {
		when(store.addUserMessage(CONVERSATION, question)).thenReturn(message(ChatRole.USER, question));
		when(store.recent(eq(CONVERSATION), anyInt())).thenReturn(history);
		return service.send(CONVERSATION, question);
	}

	private static int anyInt() {
		return org.mockito.ArgumentMatchers.anyInt();
	}

	private void streams(String... tokens) {
		when(chatPort.stream(any(), any())).thenAnswer(call -> {
			ChatStreamListener listener = call.getArgument(1);
			StringBuilder text = new StringBuilder();
			boolean interrupted = false;
			for (String token : tokens) {
				text.append(token);
				if (!listener.onToken(token)) {
					interrupted = true;
					break;
				}
			}
			return new ChatReply(text.toString(), USAGE, interrupted);
		});
	}

	@Test
	void savesTheQuestionAndStreamsTheAnswer() {
		streams("Claro", ", vamos lá");
		ChatMessage saved = message(ChatRole.ASSISTANT, "Claro, vamos lá");
		when(store.addAssistantMessage(CONVERSATION, "Claro, vamos lá", USAGE, false)).thenReturn(saved);
		PendingReply pending = pending("me ajuda?", List.of(message(ChatRole.USER, "me ajuda?")));

		pending.run(recorder);

		assertThat(pending.userMessage().content()).isEqualTo("me ajuda?");
		assertThat(recorder.tokens).containsExactly("Claro", ", vamos lá");
		assertThat(recorder.done).isSameAs(saved);
		assertThat(recorder.error).isNull();
	}

	@Test
	void keepsWhatWasAlreadySentWhenTheListenerLeaves() {
		streams("um ", "dois ", "três");
		recorder.stopAfter = 2;
		PendingReply pending = pending("conta", List.of(message(ChatRole.USER, "conta")));

		pending.run(recorder);

		verify(store).addAssistantMessage(CONVERSATION, "um dois ", USAGE, true);
	}

	@Test
	void savesThePartialTextAsInterruptedWhenTheProviderDropsMidway() {
		when(chatPort.stream(any(), any())).thenAnswer(call -> {
			ChatStreamListener listener = call.getArgument(1);
			listener.onToken("metade");
			throw new AiCallException("Falha ao chamar OpenAI para o chat", null);
		});
		PendingReply pending = pending("oi", List.of(message(ChatRole.USER, "oi")));

		pending.run(recorder);

		verify(store).addAssistantMessage(CONVERSATION, "metade", AiUsage.NONE, true);
		assertThat(recorder.error).contains("Falha ao chamar OpenAI");
	}

	@Test
	void savesNothingWhenTheProviderFailsBeforeAnyToken() {
		when(chatPort.stream(any(), any())).thenThrow(new AiCallException("Nenhum provedor de IA configurado.", null));
		PendingReply pending = pending("oi", List.of(message(ChatRole.USER, "oi")));

		pending.run(recorder);

		verify(store, never()).addAssistantMessage(any(), any(), any(), anyBoolean());
		assertThat(recorder.error).contains("Nenhum provedor");
	}

	@Test
	void reportsAnEmptyAnswerAsAnError() {
		when(chatPort.stream(any(), any())).thenReturn(new ChatReply("  ", USAGE, false));
		PendingReply pending = pending("oi", List.of(message(ChatRole.USER, "oi")));

		pending.run(recorder);

		verify(store, never()).addAssistantMessage(any(), any(), any(), anyBoolean());
		assertThat(recorder.error).isEqualTo(ChatReplyServiceImpl.EMPTY_REPLY);
	}

	@Test
	void sendsTheHistoryInOrderToTheAi() {
		streams("ok");
		List<ChatMessage> history = List.of(message(ChatRole.USER, "primeira"), message(ChatRole.ASSISTANT, "resposta"),
				message(ChatRole.USER, "segunda"));
		pending("segunda", history).run(recorder);

		ArgumentCaptor<ChatBrief> brief = ArgumentCaptor.forClass(ChatBrief.class);
		verify(chatPort).stream(brief.capture(), any());
		assertThat(brief.getValue().history()).extracting(ChatTurn::text).containsExactly("primeira", "resposta", "segunda");
		assertThat(brief.getValue().history().get(1).speaker()).isEqualTo(ChatTurn.Speaker.ASSISTANT);
	}

	@Test
	void dropsTheOldestMessagesWhenTheHistoryIsOverBudget() {
		streams("ok");
		String big = "x".repeat(ChatReplyServiceImpl.HISTORY_CHAR_BUDGET / 2 + 1);
		List<ChatMessage> history = List.of(message(ChatRole.USER, "antiga"), message(ChatRole.ASSISTANT, big),
				message(ChatRole.USER, big));
		pending("pergunta", history).run(recorder);

		ArgumentCaptor<ChatBrief> brief = ArgumentCaptor.forClass(ChatBrief.class);
		verify(chatPort).stream(brief.capture(), any());
		assertThat(brief.getValue().history()).hasSize(1);
		assertThat(brief.getValue().history().getFirst().text()).isEqualTo(big);
	}

	private static final class Recorder implements ChatReplyListener {

		private final List<String> tokens = new ArrayList<>();
		private ChatMessage done;
		private String error;
		private int stopAfter = Integer.MAX_VALUE;

		@Override
		public boolean onToken(String token) {
			tokens.add(token);
			return tokens.size() < stopAfter;
		}

		@Override
		public void onDone(ChatMessage assistantMessage) {
			done = assistantMessage;
		}

		@Override
		public void onError(String message) {
			error = message;
		}
	}
}
