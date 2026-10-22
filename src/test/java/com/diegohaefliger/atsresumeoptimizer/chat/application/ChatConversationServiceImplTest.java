package com.diegohaefliger.atsresumeoptimizer.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatAttachment;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatConversation;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatConversationDetail;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.ChatRole;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.UnknownChatJobException;
import com.diegohaefliger.atsresumeoptimizer.chat.domain.UnknownConversationException;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatConversationServiceImplTest {

	private static final Instant NOW = Instant.parse("2026-10-22T12:00:00Z");
	private static final UUID JOB_ID = UUID.randomUUID();
	private static final UUID VERSION_ID = UUID.randomUUID();

	@Mock
	private ChatConversationRepository conversations;

	@Mock
	private ChatAttachmentRepository attachments;

	@Mock
	private ChatMessageRepository messages;

	@Mock
	private JobStructuringService jobService;

	@Mock
	private ResumeService resumeService;

	@Mock
	private JobOffer offer;

	private ChatConversationServiceImpl service() {
		return new ChatConversationServiceImpl(conversations, attachments, messages,
				Mappers.getMapper(ChatEntityMapper.class), jobService, resumeService, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	private ChatConversationEntity existingConversation() {
		ChatConversationEntity entity = new ChatConversationEntity("Entrevista", NOW.minusSeconds(60));
		when(conversations.findById(entity.getId())).thenReturn(Optional.of(entity));
		return entity;
	}

	@Test
	void createsAConversationWithTheDefaultTitleWhenNoneIsGiven() {
		when(conversations.save(any(ChatConversationEntity.class))).thenAnswer(call -> call.getArgument(0));

		ChatConversation conversation = service().create("  ");

		assertThat(conversation.title()).isEqualTo(ChatConversationServiceImpl.DEFAULT_TITLE);
		assertThat(conversation.createdAt()).isEqualTo(NOW);
	}

	@Test
	void keepsTheGivenTitleTrimmed() {
		when(conversations.save(any(ChatConversationEntity.class))).thenAnswer(call -> call.getArgument(0));

		assertThat(service().create("  Fit cultural ").title()).isEqualTo("Fit cultural");
	}

	@Test
	void listsTheMostRecentlyUpdatedFirst() {
		ChatConversationEntity recent = new ChatConversationEntity("recente", NOW);
		ChatConversationEntity old = new ChatConversationEntity("antiga", NOW.minusSeconds(3600));
		when(conversations.findAllByOrderByUpdatedAtDesc()).thenReturn(List.of(recent, old));

		assertThat(service().list()).extracting(ChatConversation::title).containsExactly("recente", "antiga");
	}

	@Test
	void returnsTheConversationWithAttachmentsAndMessages() {
		ChatConversationEntity conversation = existingConversation();
		UUID id = conversation.getId();
		when(attachments.findByConversationIdOrderByCreatedAtAsc(id))
				.thenReturn(List.of(new ChatAttachmentEntity(id, JOB_ID, VERSION_ID, NOW)));
		when(messages.findByConversationIdOrderByCreatedAtAsc(id))
				.thenReturn(List.of(new ChatMessageEntity(id, ChatRole.USER, "oi", NOW)));

		ChatConversationDetail detail = service().get(id);

		assertThat(detail.title()).isEqualTo("Entrevista");
		assertThat(detail.attachments()).containsExactly(new ChatAttachment(JOB_ID, VERSION_ID));
		assertThat(detail.messages()).singleElement().satisfies(message -> {
			assertThat(message.role()).isEqualTo(ChatRole.USER);
			assertThat(message.content()).isEqualTo("oi");
		});
	}

	@Test
	void failsForAnUnknownConversation() {
		UUID id = UUID.randomUUID();
		when(conversations.findById(id)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().get(id)).isInstanceOf(UnknownConversationException.class);
	}

	@Test
	void renamesAndBumpsTheUpdateTime() {
		ChatConversationEntity conversation = existingConversation();

		ChatConversation renamed = service().rename(conversation.getId(), " Novo nome ");

		assertThat(renamed.title()).isEqualTo("Novo nome");
		assertThat(renamed.updatedAt()).isEqualTo(NOW);
	}

	@Test
	void deletesAnExistingConversation() {
		ChatConversationEntity conversation = existingConversation();

		service().delete(conversation.getId());

		verify(conversations).delete(conversation);
	}

	@Test
	void attachesAJobWithItsResumeVersion() {
		ChatConversationEntity conversation = existingConversation();
		UUID id = conversation.getId();
		when(jobService.offer(JOB_ID)).thenReturn(Optional.of(offer));
		when(attachments.findByConversationIdAndJobPostingId(id, JOB_ID)).thenReturn(Optional.empty());
		when(attachments.save(any(ChatAttachmentEntity.class))).thenAnswer(call -> call.getArgument(0));

		ChatAttachment attachment = service().attach(id, JOB_ID, VERSION_ID);

		assertThat(attachment).isEqualTo(new ChatAttachment(JOB_ID, VERSION_ID));
		verify(resumeService).getVersionText(VERSION_ID);
	}

	@Test
	void attachingTheSameJobAgainOnlySwitchesTheResumeVersion() {
		ChatConversationEntity conversation = existingConversation();
		UUID id = conversation.getId();
		ChatAttachmentEntity existing = new ChatAttachmentEntity(id, JOB_ID, null, NOW);
		when(jobService.offer(JOB_ID)).thenReturn(Optional.of(offer));
		when(attachments.findByConversationIdAndJobPostingId(id, JOB_ID)).thenReturn(Optional.of(existing));
		when(attachments.save(existing)).thenReturn(existing);

		assertThat(service().attach(id, JOB_ID, VERSION_ID).resumeVersionId()).isEqualTo(VERSION_ID);
	}

	@Test
	void refusesToAttachAnUnknownJob() {
		ChatConversationEntity conversation = existingConversation();
		when(jobService.offer(JOB_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service().attach(conversation.getId(), JOB_ID, null))
				.isInstanceOf(UnknownChatJobException.class);
		verify(attachments, never()).save(any());
	}

	@Test
	void detachesAJob() {
		ChatConversationEntity conversation = existingConversation();

		service().detach(conversation.getId(), JOB_ID);

		verify(attachments).deleteByConversationIdAndJobPostingId(conversation.getId(), JOB_ID);
	}
}
