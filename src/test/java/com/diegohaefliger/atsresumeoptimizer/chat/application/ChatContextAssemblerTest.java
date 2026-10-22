package com.diegohaefliger.atsresumeoptimizer.chat.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.chat.application.tool.JobDescription;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionText;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChatContextAssemblerTest {

	private static final UUID CONVERSATION = UUID.randomUUID();
	private static final UUID JOB_ID = UUID.randomUUID();
	private static final UUID VERSION_ID = UUID.randomUUID();
	private static final JobOffer OFFER = new JobOffer(JOB_ID, 7L, "Dev Kotlin", "Beta", null, null, null, null, null, null,
			List.of(), null, null, "Vaga de Kotlin.");

	private final ChatAttachmentRepository attachments = mock(ChatAttachmentRepository.class);
	private final JobStructuringService jobService = mock(JobStructuringService.class);
	private final ResumeService resumeService = mock(ResumeService.class);
	private final ChatContextAssembler assembler = new ChatContextAssembler(attachments, jobService,
			new JobDescription(jobService, resumeService), List.of());

	@Test
	void tellsTheModelWhenNoJobIsAttached() {
		when(attachments.findByConversationIdOrderByCreatedAtAsc(CONVERSATION)).thenReturn(List.of());

		assertThat(assembler.contextFor(CONVERSATION)).isEqualTo(ChatContextAssembler.NO_ATTACHED_JOBS);
	}

	@Test
	void describesTheAttachedJobWithTheChosenResume() {
		when(attachments.findByConversationIdOrderByCreatedAtAsc(CONVERSATION))
				.thenReturn(List.of(new ChatAttachmentEntity(CONVERSATION, JOB_ID, VERSION_ID, Instant.now())));
		when(jobService.offer(JOB_ID)).thenReturn(Optional.of(OFFER));
		when(resumeService.getVersionText(VERSION_ID)).thenReturn(new ResumeVersionText("Currículo de Fulano", null));

		String context = assembler.contextFor(CONVERSATION);

		assertThat(context).contains("Vaga V0007 — Dev Kotlin").contains("Vaga de Kotlin.").contains("Currículo de Fulano");
	}

	@Test
	void keepsTheJobWhenTheChosenResumeTextExpired() {
		when(attachments.findByConversationIdOrderByCreatedAtAsc(CONVERSATION))
				.thenReturn(List.of(new ChatAttachmentEntity(CONVERSATION, JOB_ID, VERSION_ID, Instant.now())));
		when(jobService.offer(JOB_ID)).thenReturn(Optional.of(OFFER));
		when(resumeService.getVersionText(VERSION_ID)).thenThrow(new IllegalStateException("expirou"));

		assertThat(assembler.contextFor(CONVERSATION)).contains("Dev Kotlin").contains("não está mais disponível");
	}

	@Test
	void skipsAJobThatNoLongerExists() {
		when(attachments.findByConversationIdOrderByCreatedAtAsc(CONVERSATION))
				.thenReturn(List.of(new ChatAttachmentEntity(CONVERSATION, JOB_ID, null, Instant.now())));
		when(jobService.offer(JOB_ID)).thenReturn(Optional.empty());

		assertThat(assembler.contextFor(CONVERSATION)).isEqualTo(ChatContextAssembler.NO_ATTACHED_JOBS);
	}
}
