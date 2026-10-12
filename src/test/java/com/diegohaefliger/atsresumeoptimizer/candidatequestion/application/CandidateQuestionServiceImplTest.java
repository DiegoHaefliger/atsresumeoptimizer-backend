package com.diegohaefliger.atsresumeoptimizer.candidatequestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.diegohaefliger.atsresumeoptimizer.ai.AiPort;
import com.diegohaefliger.atsresumeoptimizer.ai.AiResult;
import com.diegohaefliger.atsresumeoptimizer.ai.AiUsage;
import com.diegohaefliger.atsresumeoptimizer.ai.CandidateQuestionDraft;
import com.diegohaefliger.atsresumeoptimizer.ai.CandidateQuestionsBrief;
import com.diegohaefliger.atsresumeoptimizer.ai.CandidateQuestionsDraft;
import com.diegohaefliger.atsresumeoptimizer.candidatequestion.domain.CandidateQuestion;
import com.diegohaefliger.atsresumeoptimizer.candidatequestion.domain.CandidateQuestionSet;
import com.diegohaefliger.atsresumeoptimizer.candidatequestion.domain.CandidateQuestionsJobNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.candidatequestion.domain.CandidateQuestionsUnavailableException;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CandidateQuestionServiceImplTest {

	private static final UUID JOB_ID = UUID.randomUUID();
	private static final Instant NOW = Instant.parse("2026-10-12T12:00:00Z");
	private static final AiUsage USAGE = new AiUsage("OpenAI", "gpt-4o-mini", 600, 500, new BigDecimal("0.0010"), false);

	@Mock
	private CandidateQuestionSetRepository repository;
	@Mock
	private JobStructuringService jobStructuringService;
	@Mock
	private AiPort aiPort;

	private CandidateQuestionServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new CandidateQuestionServiceImpl(repository, new CandidateQuestionEntityMapperImpl(),
				jobStructuringService, aiPort, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void writesTheQuestionsFromTheJobTextAndBenefits() {
		when(jobStructuringService.offer(JOB_ID)).thenReturn(Optional.of(job()));
		when(aiPort.writeCandidateQuestions(any())).thenReturn(new AiResult<>(new CandidateQuestionsDraft(
				List.of(new CandidateQuestionDraft("Como funciona o home office?", "Remoto", "A vaga é híbrida"))), USAGE));
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		CandidateQuestionSet saved = service.generate(JOB_ID);

		ArgumentCaptor<CandidateQuestionsBrief> brief = ArgumentCaptor.forClass(CandidateQuestionsBrief.class);
		verify(aiPort).writeCandidateQuestions(brief.capture());
		assertThat(brief.getValue()).isEqualTo(
				new CandidateQuestionsBrief("Dev Java", "Cielo", "Sênior", "texto da vaga", List.of("VR", "Gympass")));
		assertThat(saved.jobPostingId()).isEqualTo(JOB_ID);
		assertThat(saved.aiModel()).isEqualTo("gpt-4o-mini");
		assertThat(saved.createdAt()).isEqualTo(NOW);
		assertThat(saved.questions())
				.containsExactly(new CandidateQuestion("Como funciona o home office?", "Remoto", "A vaga é híbrida"));
	}

	@Test
	void keepsAtMostSevenQuestionsAndDropsBlankOnes() {
		when(jobStructuringService.offer(JOB_ID)).thenReturn(Optional.of(job()));
		List<CandidateQuestionDraft> drafts = IntStream.rangeClosed(1, 10)
				.mapToObj(index -> new CandidateQuestionDraft(index == 1 ? " " : "Pergunta " + index, null, null))
				.toList();
		when(aiPort.writeCandidateQuestions(any())).thenReturn(new AiResult<>(new CandidateQuestionsDraft(drafts), USAGE));
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		CandidateQuestionSet saved = service.generate(JOB_ID);

		assertThat(saved.questions()).hasSize(7);
		assertThat(saved.questions().getFirst().question()).isEqualTo("Pergunta 2");
	}

	@Test
	void failsWhenTheAiReturnsNoQuestion() {
		when(jobStructuringService.offer(JOB_ID)).thenReturn(Optional.of(job()));
		when(aiPort.writeCandidateQuestions(any())).thenReturn(new AiResult<>(new CandidateQuestionsDraft(List.of()), USAGE));

		assertThatThrownBy(() -> service.generate(JOB_ID)).isInstanceOf(CandidateQuestionsUnavailableException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void refusesAnUnknownJob() {
		when(jobStructuringService.offer(JOB_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.generate(JOB_ID)).isInstanceOf(CandidateQuestionsJobNotFoundException.class);
		verifyNoInteractions(aiPort);
	}

	@Test
	void listsTheHistoryOfTheJob() {
		CandidateQuestionSetEntity entity = new CandidateQuestionSetEntity(JOB_ID, NOW);
		entity.setQuestions(List.of(new CandidateQuestionValue("Pergunta", "Time", "motivo")));
		when(repository.findByJobPostingIdOrderByCreatedAtDesc(JOB_ID)).thenReturn(List.of(entity));

		assertThat(service.list(JOB_ID)).singleElement().satisfies(
				set -> assertThat(set.questions()).extracting(CandidateQuestion::question).containsExactly("Pergunta"));
	}

	private static JobOffer job() {
		return new JobOffer(JOB_ID, 1L, "Dev Java", "Cielo", null, null, null, null, null, null,
				List.of("VR", "Gympass"), null, "Sênior", "texto da vaga");
	}
}
