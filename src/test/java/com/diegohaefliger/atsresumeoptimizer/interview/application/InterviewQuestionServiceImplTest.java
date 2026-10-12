package com.diegohaefliger.atsresumeoptimizer.interview.application;

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
import com.diegohaefliger.atsresumeoptimizer.ai.InterviewQuestionDraft;
import com.diegohaefliger.atsresumeoptimizer.ai.InterviewQuestionsBrief;
import com.diegohaefliger.atsresumeoptimizer.ai.InterviewQuestionsDraft;
import com.diegohaefliger.atsresumeoptimizer.analysis.application.AnalysisQueryService;
import com.diegohaefliger.atsresumeoptimizer.interview.domain.InterviewJobNotFoundException;
import com.diegohaefliger.atsresumeoptimizer.interview.domain.InterviewQuestion;
import com.diegohaefliger.atsresumeoptimizer.interview.domain.InterviewQuestionSet;
import com.diegohaefliger.atsresumeoptimizer.interview.domain.InterviewQuestionsUnavailableException;
import com.diegohaefliger.atsresumeoptimizer.interview.domain.InterviewRequiresJobResumeException;
import com.diegohaefliger.atsresumeoptimizer.job.JobOffer;
import com.diegohaefliger.atsresumeoptimizer.job.JobStructuringService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeService;
import com.diegohaefliger.atsresumeoptimizer.resume.ResumeVersionText;
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
class InterviewQuestionServiceImplTest {

	private static final UUID JOB_ID = UUID.randomUUID();
	private static final UUID VERSION_ID = UUID.randomUUID();
	private static final Instant NOW = Instant.parse("2026-10-08T12:00:00Z");
	private static final AiUsage USAGE = new AiUsage("OpenAI", "gpt-4o-mini", 800, 900, new BigDecimal("0.0020"), false);

	@Mock
	private InterviewQuestionSetRepository repository;
	@Mock
	private AnalysisQueryService analysisQueryService;
	@Mock
	private JobStructuringService jobStructuringService;
	@Mock
	private ResumeService resumeService;
	@Mock
	private AiPort aiPort;

	private InterviewQuestionServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new InterviewQuestionServiceImpl(repository, new InterviewQuestionEntityMapperImpl(), analysisQueryService,
				jobStructuringService, resumeService, aiPort, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void writesTheQuestionsFromTheJobRequirementsAndTheChosenResume() {
		jobWithKeywords();
		when(analysisQueryService.analysisOfGeneratedVersion(JOB_ID, VERSION_ID)).thenReturn(Optional.of(UUID.randomUUID()));
		when(resumeService.getVersionText(VERSION_ID)).thenReturn(new ResumeVersionText("currículo adaptado", null));
		when(aiPort.writeInterviewQuestions(any())).thenReturn(new AiResult<>(new InterviewQuestionsDraft(List.of(
				new InterviewQuestionDraft("Como o Kafka garante ordem?", "Kafka", "A vaga pede Kafka", "Cite o projeto X"))),
				USAGE));
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		InterviewQuestionSet saved = service.generate(JOB_ID, VERSION_ID);

		ArgumentCaptor<InterviewQuestionsBrief> brief = ArgumentCaptor.forClass(InterviewQuestionsBrief.class);
		verify(aiPort).writeInterviewQuestions(brief.capture());
		assertThat(brief.getValue()).isEqualTo(new InterviewQuestionsBrief("Dev Java", "Cielo", "Sênior", "texto da vaga",
				List.of("Java", "Kafka"), List.of("Java", "Kafka", "Docker"), "currículo adaptado"));
		assertThat(saved.jobPostingId()).isEqualTo(JOB_ID);
		assertThat(saved.resumeVersionId()).isEqualTo(VERSION_ID);
		assertThat(saved.aiModel()).isEqualTo("gpt-4o-mini");
		assertThat(saved.createdAt()).isEqualTo(NOW);
		assertThat(saved.questions()).containsExactly(
				new InterviewQuestion("Como o Kafka garante ordem?", "Kafka", "A vaga pede Kafka", "Cite o projeto X"));
	}

	@Test
	void writesTheQuestionsWithoutAResume() {
		jobWithKeywords();
		when(aiPort.writeInterviewQuestions(any())).thenReturn(new AiResult<>(new InterviewQuestionsDraft(
				List.of(new InterviewQuestionDraft("O que é JPA?", "JPA", null, null))), USAGE));
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		InterviewQuestionSet saved = service.generate(JOB_ID, null);

		ArgumentCaptor<InterviewQuestionsBrief> brief = ArgumentCaptor.forClass(InterviewQuestionsBrief.class);
		verify(aiPort).writeInterviewQuestions(brief.capture());
		assertThat(brief.getValue().resumeText()).isNull();
		assertThat(saved.resumeVersionId()).isNull();
		verifyNoInteractions(resumeService, analysisQueryService);
	}

	@Test
	void keepsAtMostTenQuestionsAndDropsBlankOnes() {
		jobWithKeywords();
		List<InterviewQuestionDraft> drafts = IntStream.rangeClosed(1, 12)
				.mapToObj(index -> new InterviewQuestionDraft(index == 1 ? " " : "Pergunta " + index, null, null, null))
				.toList();
		when(aiPort.writeInterviewQuestions(any())).thenReturn(new AiResult<>(new InterviewQuestionsDraft(drafts), USAGE));
		when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

		InterviewQuestionSet saved = service.generate(JOB_ID, null);

		assertThat(saved.questions()).hasSize(10);
		assertThat(saved.questions().getFirst().question()).isEqualTo("Pergunta 2");
	}

	@Test
	void failsWhenTheAiReturnsNoQuestion() {
		jobWithKeywords();
		when(aiPort.writeInterviewQuestions(any())).thenReturn(new AiResult<>(new InterviewQuestionsDraft(List.of()), USAGE));

		assertThatThrownBy(() -> service.generate(JOB_ID, null)).isInstanceOf(InterviewQuestionsUnavailableException.class);
		verify(repository, never()).save(any());
	}

	@Test
	void refusesAResumeThatWasNotGeneratedForTheJob() {
		when(jobStructuringService.offer(JOB_ID)).thenReturn(Optional.of(job()));
		when(analysisQueryService.analysisOfGeneratedVersion(JOB_ID, VERSION_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.generate(JOB_ID, VERSION_ID))
				.isInstanceOf(InterviewRequiresJobResumeException.class);
		verifyNoInteractions(aiPort, repository);
	}

	@Test
	void refusesAnUnknownJob() {
		when(jobStructuringService.offer(JOB_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.generate(JOB_ID, null)).isInstanceOf(InterviewJobNotFoundException.class);
		verifyNoInteractions(aiPort);
	}

	@Test
	void listsTheHistoryOfTheJob() {
		InterviewQuestionSetEntity entity = new InterviewQuestionSetEntity(JOB_ID, null, NOW);
		entity.setQuestions(List.of(new InterviewQuestionValue("Pergunta", "Java", "motivo", "dica")));
		when(repository.findByJobPostingIdOrderByCreatedAtDesc(JOB_ID)).thenReturn(List.of(entity));

		assertThat(service.list(JOB_ID)).singleElement()
				.satisfies(set -> assertThat(set.questions()).extracting(InterviewQuestion::question).containsExactly("Pergunta"));
	}

	private void jobWithKeywords() {
		when(jobStructuringService.offer(JOB_ID)).thenReturn(Optional.of(job()));
		when(jobStructuringService.selectedKeywords(JOB_ID)).thenReturn(List.of("Java", "Kafka"));
		when(jobStructuringService.keywords(JOB_ID)).thenReturn(List.of("Java", "Kafka", "Docker"));
	}

	private static JobOffer job() {
		return new JobOffer(JOB_ID, 1L, "Dev Java", "Cielo", null, null, null, null, null, null, List.of(), null, "Sênior",
				"texto da vaga");
	}
}
