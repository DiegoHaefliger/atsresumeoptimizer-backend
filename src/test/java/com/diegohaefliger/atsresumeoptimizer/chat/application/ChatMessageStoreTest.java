package com.diegohaefliger.atsresumeoptimizer.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatMessage;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatRole;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.NothingToRegenerateException;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.UnknownConversationException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class ChatMessageStoreTest {

	private static final Instant NOW = Instant.parse("2026-10-22T12:00:00Z");

	private final ChatConversationRepository conversations = mock(ChatConversationRepository.class);
	private final ChatMessageRepository messages = mock(ChatMessageRepository.class);
	private final ChatMessageStore store = new ChatMessageStore(conversations, messages,
			Mappers.getMapper(ChatEntityMapper.class), Clock.fixed(NOW, ZoneOffset.UTC));

	private ChatConversationEntity conversation(String title) {
		ChatConversationEntity entity = new ChatConversationEntity(title, NOW.minusSeconds(600));
		when(conversations.findById(entity.getId())).thenReturn(Optional.of(entity));
		when(messages.save(any(ChatMessageEntity.class))).thenAnswer(call -> call.getArgument(0));
		return entity;
	}

	@Test
	void namesAFreshConversationAfterTheFirstQuestion() {
		ChatConversationEntity entity = conversation(ChatConversationServiceImpl.DEFAULT_TITLE);

		store.addUserMessage(entity.getId(), "Como me preparo para a entrevista?\nTenho pouco tempo.");

		assertThat(entity.getTitle()).isEqualTo("Como me preparo para a entrevista?");
		assertThat(entity.getUpdatedAt()).isEqualTo(NOW);
	}

	@Test
	void shortensALongFirstQuestionInTheTitle() {
		ChatConversationEntity entity = conversation(ChatConversationServiceImpl.DEFAULT_TITLE);

		store.addUserMessage(entity.getId(), "a".repeat(200));

		assertThat(entity.getTitle()).hasSize(60).endsWith("…");
	}

	@Test
	void keepsTheTitleOnceTheConversationHasMessages() {
		ChatConversationEntity entity = conversation(ChatConversationServiceImpl.DEFAULT_TITLE);
		when(messages.existsByConversationId(entity.getId())).thenReturn(true);

		store.addUserMessage(entity.getId(), "outra pergunta");

		assertThat(entity.getTitle()).isEqualTo(ChatConversationServiceImpl.DEFAULT_TITLE);
		assertThat(entity.getUpdatedAt()).isEqualTo(NOW);
	}

	@Test
	void keepsATitleTheUserChose() {
		ChatConversationEntity entity = conversation("Fit cultural");

		store.addUserMessage(entity.getId(), "oi");

		assertThat(entity.getTitle()).isEqualTo("Fit cultural");
	}

	@Test
	void recordsModelAndCostOnTheAnswer() {
		ChatConversationEntity entity = conversation("Entrevista");
		com.diegohaefliger.atsresumeoptimizer.ai.AiUsage usage = new com.diegohaefliger.atsresumeoptimizer.ai.AiUsage(
				"OpenAI", "gpt-4o", 100, 50, new java.math.BigDecimal("0.002"), false);

		ChatMessage saved = store.addAssistantMessage(entity.getId(), "resposta", usage, true);

		assertThat(saved.aiModel()).isEqualTo("gpt-4o");
		assertThat(saved.tokensIn()).isEqualTo(100);
		assertThat(saved.costUsd()).isEqualByComparingTo("0.002");
		assertThat(saved.interrupted()).isTrue();
	}

	private void givenNewestFirst(ChatConversationEntity conversation, ChatMessageEntity... newestFirst) {
		when(messages.findByConversationIdOrderByCreatedAtDesc(any(), any())).thenReturn(new ArrayList<>(List.of(newestFirst)));
	}

	@Test
	void dropsTheLastAnswerAndReopensTheQuestionToRegenerate() {
		ChatConversationEntity conversation = conversation("Entrevista");
		ChatMessageEntity answer = new ChatMessageEntity(conversation.getId(), ChatRole.ASSISTANT, "resposta ruim", NOW);
		ChatMessageEntity question = new ChatMessageEntity(conversation.getId(), ChatRole.USER, "minha pergunta", NOW);
		givenNewestFirst(conversation, answer, question);

		ChatMessage reopened = store.reopenLastQuestion(conversation.getId());

		assertThat(reopened.content()).isEqualTo("minha pergunta");
		verify(messages).delete(answer);
	}

	@Test
	void reopensAQuestionThatNeverGotAnAnswer() {
		ChatConversationEntity conversation = conversation("Entrevista");
		ChatMessageEntity question = new ChatMessageEntity(conversation.getId(), ChatRole.USER, "sem resposta", NOW);
		givenNewestFirst(conversation, question);

		assertThat(store.reopenLastQuestion(conversation.getId()).content()).isEqualTo("sem resposta");
		verify(messages, never()).delete(any(ChatMessageEntity.class));
	}

	@Test
	void refusesToRegenerateAnEmptyConversation() {
		ChatConversationEntity conversation = conversation("Entrevista");
		givenNewestFirst(conversation);

		assertThatThrownBy(() -> store.reopenLastQuestion(conversation.getId())).isInstanceOf(NothingToRegenerateException.class);
	}

	@Test
	void refusesToRegenerateWhenOnlyAnAnswerRemains() {
		ChatConversationEntity conversation = conversation("Entrevista");
		givenNewestFirst(conversation, new ChatMessageEntity(conversation.getId(), ChatRole.ASSISTANT, "solta", NOW));

		assertThatThrownBy(() -> store.reopenLastQuestion(conversation.getId())).isInstanceOf(NothingToRegenerateException.class);
	}

	@Test
	void refusesAQuestionForAnUnknownConversation() {
		UUID id = UUID.randomUUID();
		when(conversations.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> store.addUserMessage(id, "oi")).isInstanceOf(UnknownConversationException.class);
	}
}
